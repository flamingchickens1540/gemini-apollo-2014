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
    private BooleanInputPoll kinectTrigger;
    // Tuned constants are below near the autonomous modes.
    private final StringHolder option = new StringHolder("hotcheck");
    private final String[] options = {"none", "forward", "hotcheck", "double"};
    private final CList optionList = CArrayUtils.asList(options);
    private final Event fireWhenEvent = new Event(), rearmWhenEvent = new Event(), notifyRearm = new Event();

    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        String cur = option.get();
        if ("none".equals(cur)) {
            autoNone();
        } else if ("forward".equals(cur)) {
            autoForward();
        } else if ("hotcheck".equals(cur)) {
            autoHotcheck();
        } else if ("double".equals(cur)) {
            autoDouble();
        } else {
            Logger.severe("Nonexistent autonomous mode: " + option.get());
        }
    }

    // *** Modes ***
    private void autoNone() throws AutonomousModeOverException, InterruptedException {
        rightDrive.writeValue(0);
        leftDrive.writeValue(0);
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

    private final FloatStatus hotcheckBeforeMove = tune.getFloat("autom-hotcheck-before-move", 0);
    private final FloatStatus hotcheckMoveSpeed = tune.getFloat("autom-hotcheck-move-speed", -0.8f);
    private final FloatStatus hotcheckMoveLength = tune.getFloat("autom-hotcheck-move-length", 4);
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
            arm.writeValue(true);
            leftDrive.writeValue(0);
            rightDrive.writeValue(0);
            float target = currentTime.readValue() + hotcheckMoveLength.readValue();
            BooleanInputPoll fatl = Mixing.floatIsAtLeast(currentTime, target);
            int i = waitUntilOneOf(new BooleanInputPoll[]{kinectTrigger, fatl});
            if (i != 0) {
                Logger.warning("Cancelled wait for HotZone after " + hotcheckMoveLength.readValue() + " seconds: " + currentTime.readValue() + " and " + target);
            }
        }
        Logger.fine("Found hotzone");
        {
            collect.writeValue(hotcheckCollector.readValue());
            leftDrive.writeValue(hotcheckMoveSpeed.readValue());
            rightDrive.writeValue(hotcheckMoveSpeed.readValue());
            waitForTime(hotcheckBeforeMove);
        }
        Logger.fine("Ended pretime");
        {
            leftDrive.writeValue(0);
            rightDrive.writeValue(0);
        }
        Logger.fine("Arrived");
        waitForTime(hotcheckArmMoveTime);
        arm.writeValue(false);
        Logger.fine("Up");
        waitForTime(hotcheckArmMoveTime);
        arm.writeValue(true);
        Logger.fine("Down");
        waitForTime(hotcheckPreFireDelay);
        fireWhenEvent.produce();
        Logger.fine("Fired.");
        {
            waitForTime(hotcheckPreDelay);
            Logger.fine("Wait over.");
            leftDrive.writeValue(hotcheckMovement.readValue());
            rightDrive.writeValue(hotcheckMovement.readValue());
            waitForTime(hotcheckMoveDelay);
            leftDrive.writeValue(0);
            rightDrive.writeValue(0);
        }
    }

    private final FloatStatus doubleArmMoveTime = tune.getFloat("autom-double-armmove-time", 0.6f);
    private final FloatStatus doubleFireTime = tune.getFloat("autom-double-fire-time", 1.1f);
    private final FloatStatus doubleCollectTime = tune.getFloat("autom-double-collect-time", 1.1f);

    private void autoDouble() throws InterruptedException, AutonomousModeOverException {
        Logger.fine("Began double mode!");
        arm.writeValue(true);
        waitForTime(doubleArmMoveTime);
        Logger.fine("Arm moved - firing!");
        fireWhenEvent.produce();
        waitForTime(doubleFireTime);
        rearmWhenEvent.produce();
        Logger.fine("Rearming...");
        waitForEvent(notifyRearm);
        Logger.fine("Rearmed!");
        collect.writeValue(1f);
        waitForTime(doubleCollectTime);
        collect.writeValue(0);
        Logger.fine("Collected - firing!");
        fireWhenEvent.produce();
        waitForTime(doubleFireTime);
        arm.writeValue(false);
        Logger.fine("Double completed.");
    }

    // *** Framework ***
    private void waitForTime(FloatInputPoll fin) throws InterruptedException, AutonomousModeOverException {
        waitForTime((long) (1000L * fin.readValue() + 0.5f));
    }

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
                    sb.append("BUTTON ").append(options[i]).append('\n');
                }
                openDialog.invoke(sb.toString().getBytes(), new ByteArrayOutputStream() {
                    public void close() {
                        String str = new String(this.toByteArray());
                        if (str.length() > 0 && optionList.indexOf(str) != -1) {
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

    public void putKinectTrigger(BooleanInputPoll kinectTrigger) {
        this.kinectTrigger = kinectTrigger;
    }

    public EventSource getWhenToFire() {
        return fireWhenEvent;
    }

    public EventSource getWhenToRearm() {
        return rearmWhenEvent;
    }

    public void putArm(BooleanOutput armSolenoid, FloatOutput collector) {
        arm = armSolenoid;
        collect = collector;
    }

    public EventConsumer getNotifyRearmFinished() {
        return notifyRearm;
    }
}
