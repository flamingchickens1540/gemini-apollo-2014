package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.StringHolder;
import ccre.holders.TuningContext;
import ccre.instinct.*;
import ccre.log.Logger;
import ccre.saver.StorageProvider;
import ccre.saver.StorageSegment;
import ccre.util.Utils;

public class AutonomousController extends InstinctModule {

    private final StorageSegment seg = StorageProvider.openStorage("autonomous");
    private final TuningContext tune = new TuningContext(CluckGlobals.node, seg).publishSavingEvent("Autonomous");
    // Provided channels
    private FloatOutput leftDrive, rightDrive;
    private BooleanInputPoll isHotzone;
    // Tuned constants are below near the autonomous modes.
    private final StringHolder option = new StringHolder("none");
    private final String[] options = {"none", "forward", "hotcheck"};
    private final Event fireWhenEvent = new Event();

    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        String cur = option.get();
        if (cur.equals("none")) {
            autoNone();
        } else if (cur.equals("forward")) {
            autoForward();
        } else if (cur.equals("hotcheck")) {
            autoHotcheck();
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

    private final FloatStatus hotcheckMaxDelay = tune.getFloat("autom-hotcheck-maxwait", 6);
    private final FloatStatus hotcheckPreDelay = tune.getFloat("autom-hotcheck-fire-wait", 0.5f);
    private final FloatStatus hotcheckMovement = tune.getFloat("autom-hotcheck-move-speed", 0.5f);
    private final FloatStatus hotcheckMoveDelay = tune.getFloat("autom-hotcheck-move-duration", 0);

    private void autoHotcheck() throws AutonomousModeOverException, InterruptedException {
        FloatInputPoll cur = Utils.currentTimeSeconds;
        float target = cur.readValue() + hotcheckMaxDelay.readValue(); // Wait six seconds at most.
        BooleanInputPoll fatl = Mixing.floatIsAtLeast(cur, target);
        int i = waitUntilOneOf(new BooleanInputPoll[]{isHotzone, fatl});
        if (i != 0) {
            Logger.warning("Cancelled wait for HotZone after " + hotcheckMaxDelay.readValue() + " seconds: " + cur.readValue() + " and " + target);
        }
        fireWhenEvent.produce();
        waitForTime((long) (1000L * hotcheckPreDelay.readValue() + 0.5f));
        leftDrive.writeValue(hotcheckMovement.readValue());
        rightDrive.writeValue(hotcheckMovement.readValue());
        waitForTime((long) (1000L * hotcheckMoveDelay.readValue() + 0.5f));
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
                int i;
                String cur = option.get();
                for (i = 1; i < options.length; i++) {
                    if (cur.equals(options[i - 1])) {
                        option.set(options[i]);
                        sayCurrent();
                        return;
                    }
                }
                if (cur.equals(options[options.length - 1])) {
                    option.set(options[0]);
                    sayCurrent();
                } else {
                    Logger.warning("Invalid autonomous mode: " + cur + ": resetting.");
                }
            }
        });
        CluckGlobals.node.publish("autom-check", new EventConsumer() {
            public void eventFired() {
                sayCurrent();
            }
        });
        CluckGlobals.node.publish("autom-prev", new EventConsumer() {
            public void eventFired() {
                int i;
                String cur = option.get();
                for (i = 1; i < options.length; i++) {
                    if (cur.equals(options[i])) {
                        option.set(options[i - 1]);
                        sayCurrent();
                        return;
                    }
                }
                if (cur.equals(options[0])) {
                    option.set(options[options.length - 1]);
                    sayCurrent();
                } else {
                    Logger.warning("Invalid autonomous mode: " + cur + ": resetting.");
                }
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

    public EventSource getWhenToFire() {
        return fireWhenEvent;
    }
}
