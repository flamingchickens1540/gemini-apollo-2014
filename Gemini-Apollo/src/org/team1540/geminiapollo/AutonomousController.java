package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.cluck.rpc.RemoteProcedure;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.*;
import ccre.instinct.*;
import ccre.log.Logger;
import ccre.saver.*;
import ccre.util.*;
import java.io.ByteArrayOutputStream;

public class AutonomousController extends InstinctModule {

    private final StorageSegment seg = StorageProvider.openStorage("autonomous");
    private final TuningContext tune = new TuningContext(CluckGlobals.node, seg).publishSavingEvent("Autonomous");
    // Provided channels
    private FloatOutput leftDrive, rightDrive, collect;
    private BooleanOutput arm;
    private BooleanInputPoll isHotzone;
    private FloatInputPoll ultrasonicSensor;
    // Tuned constants are below near the autonomous modes.
    private final StringHolder option = new StringHolder("hotcheck");
    private final String[] options = {"none", "forward", "hotcheck", "ultrasonic"};
    private final CList optionList = CArrayUtils.asList(options);
    private final Event fireWhenEvent = new Event();

    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        String cur = option.get();
        if (cur.equals("none")) {
            autoNone();
        } else if (cur.equals("forward")) {
            autoForward();
        } else if (cur.equals("hotcheck")) {
            autoHotcheck();
        } else if (cur.equals("ultrasonic")) {
            autoUltrasonic();
        } else {
            Logger.severe("Nonexistent autonomous mode: " + option.get());
        }
    }

    // *** Modes ***
    private void autoNone() throws AutonomousModeOverException, InterruptedException {
        rightDrive.writeValue(0);
        leftDrive.writeValue(0);
        // Really. Just do nothing.
    }

    private final FloatStatus forwardMovement = tune.getFloat("autom-forward-speed", 0.5f);
    private final FloatStatus forwardDelay = tune.getFloat("autom-forward-delay", 0.5f);

    private void autoForward() throws AutonomousModeOverException, InterruptedException {
        float speed = forwardMovement.readValue(); // [0, 1]
        float delay = forwardDelay.readValue(); // In seconds
        rightDrive.writeValue(speed);
        leftDrive.writeValue(speed);
        waitForTime((long) (1000L * delay + 0.5f)); // Round to nearest integer.
        rightDrive.writeValue(0);
        leftDrive.writeValue(0);
    }

    private final FloatStatus hotcheckPreUltraDelay = tune.getFloat("autom-hotcheck-pru-delay", 0);
    private final FloatStatus hotcheckUltraSpeed = tune.getFloat("autom-hotcheck-ult-speed", -0.8f);
    private final FloatStatus hotcheckUltraMaxDelay = tune.getFloat("autom-hotcheck-ult-max-delay", 1.5f);
    private final FloatStatus hotcheckUltraEnd = tune.getFloat("autom-hotcheck-ult-end", 1.54f);
    private final FloatStatus hotcheckMaxDelay = tune.getFloat("autom-hotcheck-maxwait", 4);
    private final FloatStatus hotcheckPreDelay = tune.getFloat("autom-hotcheck-fire-wait", 0.5f);
    private final FloatStatus hotcheckMovement = tune.getFloat("autom-hotcheck-move-speed", -0.7f);
    private final FloatStatus hotcheckMoveDelay = tune.getFloat("autom-hotcheck-move-duration", 0);
    private final FloatStatus hotcheckCollector = tune.getFloat("autom-hotcheck-collector", 0.5f);
    private final FloatStatus hotcheckPreFireDelay = tune.getFloat("autom-hotcheck-prefire-delay", 1);
    private final FloatStatus hotcheckArmMoveTime = tune.getFloat("autom-hotcheck-armmove-time", 0.6f);

    private void autoHotcheck() throws AutonomousModeOverException, InterruptedException {
        FloatInputPoll currentTime = Utils.currentTimeSeconds;
        Logger.fine("Began Hotcheck");
        {
            //arm.writeValue(false);
            arm.writeValue(true);
            leftDrive.writeValue(0);
            rightDrive.writeValue(0);
            float target = currentTime.readValue() + hotcheckMaxDelay.readValue(); // Wait six seconds at most.
            BooleanInputPoll fatl = Mixing.floatIsAtLeast(currentTime, target);
            int i = waitUntilOneOf(new BooleanInputPoll[]{isHotzone, fatl});
            if (i != 0) {
                Logger.warning("Cancelled wait for HotZone after " + hotcheckMaxDelay.readValue() + " seconds: " + currentTime.readValue() + " and " + target);
            }
        }
        Logger.fine("Found hotzone");
        {
            collect.writeValue(hotcheckCollector.readValue());
            leftDrive.writeValue(hotcheckUltraSpeed.readValue());
            rightDrive.writeValue(hotcheckUltraSpeed.readValue());
            waitForTime((long) (1000L * hotcheckPreUltraDelay.readValue() + 0.5f));
        }
        Logger.fine("Ended pretime");
        {
            float timeoutAt = currentTime.readValue() + hotcheckUltraMaxDelay.readValue(); // Wait one second at most.
            float lengthAt = hotcheckUltraEnd.readValue();
            int i = waitUntilOneOf(new BooleanInputPoll[]{
                Mixing.floatIsAtMost(ultrasonicSensor, lengthAt),
                Mixing.floatIsAtLeast(currentTime, timeoutAt)});
            if (i != 0) {
                Logger.warning("Cancelled wait for Ultrasonic after " + hotcheckUltraMaxDelay.readValue());
            }
            leftDrive.writeValue(0);
            rightDrive.writeValue(0);
        }
        Logger.fine("Arrived");
        waitForTime((long) (1000L * hotcheckArmMoveTime.readValue() + 0.5f));
        arm.writeValue(false);
        Logger.fine("Up");
        waitForTime((long) (1000L * hotcheckArmMoveTime.readValue() + 0.5f));
        arm.writeValue(true);
        Logger.fine("Down");
        waitForTime((long) (1000L * hotcheckPreFireDelay.readValue() + 0.5f));
        fireWhenEvent.produce();
        Logger.fine("Fired.");
        {
            waitForTime((long) (1000L * hotcheckPreDelay.readValue() + 0.5f));
            Logger.fine("Wait over.");
            leftDrive.writeValue(hotcheckMovement.readValue());
            rightDrive.writeValue(hotcheckMovement.readValue());
            waitForTime((long) (1000L * hotcheckMoveDelay.readValue() + 0.5f));
            leftDrive.writeValue(0);
            rightDrive.writeValue(0);
        }
    }

    private final FloatStatus ultrasonicMovement = tune.getFloat("autom-ultrasonic-move-speed", 0.5f);
    private final FloatStatus ultrasonicTarget = tune.getFloat("autom-ultrasonic-target", 3);

    private void autoUltrasonic() throws AutonomousModeOverException, InterruptedException {
        leftDrive.writeValue(ultrasonicMovement.readValue());
        rightDrive.writeValue(ultrasonicMovement.readValue());
        waitUntilAtMost(ultrasonicSensor, ultrasonicTarget.readValue());
        leftDrive.writeValue(0);
        rightDrive.writeValue(0);
    }

    // *** Framework ***
    private void sayCurrent() {
        Logger.info("Autonomous mode is currently set to: " + option.get());
    }

    public void setup(InstinctRegistrar reg) {
        seg.attachStringHolder("autonomous-mode", option);
        CluckGlobals.node.publish("autom-next", new EventConsumer() {
            public void eventFired() {
                option.set(options[(optionList.indexOf(option.get()) + 1) % options.length]);
                sayCurrent();
            }
        });
        CluckGlobals.node.publish("autom-check", new EventConsumer() {
            public void eventFired() {
                sayCurrent();
            }
        });
        CluckGlobals.node.publish("autom-prev", new EventConsumer() {
            public void eventFired() {
                option.set(options[(optionList.indexOf(option.get()) - 1 + options.length) % options.length]);
                sayCurrent();
            }
        });
        final RemoteProcedure openDialog = CluckGlobals.node.subscribeRP("phidget/display-dialog", 11000);
        CluckGlobals.node.publish("autom-select", new EventConsumer() {
            public void eventFired() {
                StringBuffer sb = new StringBuffer("TITLE Select Autonomous Mode\n");
                for (int i = 0; i < options.length; i++) {
                    String option = options[i];
                    sb.append("BUTTON ").append(option).append('\n');
                }
                openDialog.invoke(sb.toString().getBytes(), new ByteArrayOutputStream() {
                    public void close() {
                        String str = new String(this.toByteArray());
                        if (str.length() == 0) {
                            return;
                        }
                        int index = optionList.indexOf(str);
                        if (index != -1) {
                            option.set(str);
                            sayCurrent();
                        }
                    }
                });
            }
        });
        sayCurrent();
        register(reg);
    }

    public void putDriveMotors(FloatOutput leftDrive1, FloatOutput leftDrive2, FloatOutput rightDrive1, FloatOutput rightDrive2) {
        this.leftDrive = Mixing.combine(leftDrive1, leftDrive2);
        this.rightDrive = Mixing.combine(rightDrive1, rightDrive2);
    }

    public void putHotzone(BooleanInputPoll isHotzone) {
        this.isHotzone = isHotzone;
    }

    public void putUltrasonic(FloatInputPoll ultrasonic) {
        ultrasonicSensor = ultrasonic;
    }

    public EventSource getWhenToFire() {
        return fireWhenEvent;
    }

    public void putArm(BooleanOutput armSolenoid, FloatOutput collector) {
        arm = armSolenoid;
        collect = collector;
    }
}
