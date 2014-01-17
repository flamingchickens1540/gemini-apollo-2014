package org.team1540.geminiapollo;

import ccre.chan.FloatOutput;
import ccre.chan.FloatStatus;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.Mixing;
import ccre.event.EventConsumer;
import ccre.holders.StringHolder;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;
import ccre.instinct.InstinctRegistrar;
import ccre.log.Logger;
import ccre.saver.StorageProvider;
import ccre.saver.StorageSegment;

public class AutonomousController extends InstinctModule {

    private final StorageSegment seg = StorageProvider.openStorage("autonomous");
    private final TuningContext tune = new TuningContext(CluckGlobals.node, seg).publishSavingEvent("save-autonomous");
    private FloatOutput rightDrive;
    private FloatOutput leftDrive;
    // Tuned constants are below near the autonomous modes.
    private final StringHolder option = new StringHolder("none");
    private final String[] options = {"none", "forward"};

    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        String cur = option.get();
        if (cur.equals("none")) {
            autoNone();
        } else if (cur.equals("forward")) {
            autoForward();
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

    private final FloatStatus forwardMovement = tune.getFloat("a-forward-speed", 0.5f);
    private final FloatStatus forwardDelay = tune.getFloat("a-forward-delay", 0.5f);

    private void autoForward() throws AutonomousModeOverException, InterruptedException {
        float speed = forwardMovement.readValue(); // [0, 1]
        float delay = forwardDelay.readValue(); // In seconds
        rightDrive.writeValue(speed);
        leftDrive.writeValue(speed);
        waitForTime((long) (1000L * delay + 0.5f)); // Round to nearest integer.
        rightDrive.writeValue(0);
        leftDrive.writeValue(0);
    }

    // *** Framework ***
    private void sayCurrent() {
        Logger.info("Autonomous mode is currently set to: " + option.get());
    }

    public void setup(InstinctRegistrar reg) {
        seg.attachStringHolder("autonomous-mode", option);
        CluckGlobals.node.publish("next-autonomous", new EventConsumer() {
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
        CluckGlobals.node.publish("check-autonomous", new EventConsumer() {
            public void eventFired() {
                sayCurrent();
            }
        });
        CluckGlobals.node.publish("prev-autonomous", new EventConsumer() {
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
}
