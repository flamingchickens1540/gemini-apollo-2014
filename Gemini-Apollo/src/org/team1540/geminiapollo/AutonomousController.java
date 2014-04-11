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
import java.io.UnsupportedEncodingException;

public class AutonomousController extends InstinctModule {

    private final StorageSegment seg = StorageProvider.openStorage("autonomous");
    private final TuningContext tune = new TuningContext(CluckGlobals.getNode(), seg).publishSavingEvent("Autonomous");
    // Provided channels
    private FloatOutput bothDrive, collect;
    private BooleanOutput collectSols, useCurrent;
    private BooleanInputPoll kinectTrigger;
    private EventConsumer lowerArm, raiseArm, alignArm;
    private final Event fireWhenEvent = new Event(), rearmWhenEvent = new Event();
    // Tuned constants are below near the autonomous modes.
    private final StringHolder option = new StringHolder("double");
    private final String[] options = {"none", "forward", "hotcheck", "double"};
    private final CList optionList = CArrayUtils.asList(options);
    private final BooleanStatus winchGotten = new BooleanStatus();

    protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
        try {
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
        } finally {
            bothDrive.writeValue(0);
        }
    }

    // *** Modes ***
    private void autoNone() throws AutonomousModeOverException, InterruptedException {
        bothDrive.writeValue(0);
    }

    private final FloatStatus forwardMovement = tune.getFloat("autom-forward-speed", -1f);
    private final FloatStatus forwardDelay = tune.getFloat("autom-forward-delay", 0.5f);

    private void autoForward() throws AutonomousModeOverException, InterruptedException {
        bothDrive.writeValue(forwardMovement.readValue());
        waitForTime(forwardDelay);
        bothDrive.writeValue(0);
    }

    private final FloatStatus hotcheckAlignDistance = tune.getFloat("autom-hotcheck-align-distance", 0.3f);
    private final FloatStatus hotcheckAlignSpeed = tune.getFloat("autom-hotcheck-align-speed", -1f);
    private final FloatStatus hotcheckTimeoutAfter = tune.getFloat("autom-hotcheck-timeout", 4);
    private final FloatStatus hotcheckPostFirePause = tune.getFloat("autom-hotcheck-postfire-pause", 0.5f);
    private final FloatStatus hotcheckPostFireSpeed = tune.getFloat("autom-hotcheck-postfire-speed", -0.7f);
    private final FloatStatus hotcheckPostFireMove = tune.getFloat("autom-hotcheck-postfire-move", 0);
    private final FloatStatus hotcheckCollectorSpeed = tune.getFloat("autom-hotcheck-collector", 0.5f);
    private final FloatStatus hotcheckPreFirePause = tune.getFloat("autom-hotcheck-prefire-pause", 1);
    private final FloatStatus hotcheckArmMoveTime = tune.getFloat("autom-hotcheck-armmove-time", 0);//.6f);

    private void autoHotcheck() throws AutonomousModeOverException, InterruptedException {
        FloatInputPoll currentTime = Utils.currentTimeSeconds;
        Logger.fine("Began Hotcheck");
        bothDrive.writeValue(hotcheckAlignSpeed.readValue());
        waitForTime(hotcheckAlignDistance);
        bothDrive.writeValue(0);
        if (hotcheckArmMoveTime.readValue() > 0.02f) {
            collect.writeValue(hotcheckCollectorSpeed.readValue());
            raiseArm.eventFired();
            waitForTime(hotcheckArmMoveTime);
            Logger.fine("Up");
            lowerArm.eventFired();
            waitForTime(hotcheckArmMoveTime);
            Logger.fine("Down");
            collect.writeValue(0);
        } else {
            Logger.fine("Skip Arm");
        }
        Logger.fine("Arrived");
        alignArm.eventFired();
        float timeoutTime = currentTime.readValue() + hotcheckTimeoutAfter.readValue();
        BooleanInputPoll timedout = Mixing.floatIsAtLeast(currentTime, timeoutTime);
        if (waitUntilOneOf(new BooleanInputPoll[]{kinectTrigger, timedout}) != 0) {
            Logger.warning("Cancelled HotZone wait after " + hotcheckTimeoutAfter.readValue() + " secs: " + currentTime.readValue() + "," + timeoutTime);
        }
        Logger.fine("Found hotzone");
        waitForTime(hotcheckPreFirePause);
        fireWhenEvent.produce();
        Logger.fine("Fired.");
        waitForTime(hotcheckPostFirePause);
        Logger.fine("Firing delay over..");
        bothDrive.writeValue(hotcheckPostFireSpeed.readValue());
        waitForTime(hotcheckPostFireMove);
        bothDrive.writeValue(0);
        Logger.fine("Hotcheck done!");
    }

    private final FloatStatus doubleArmMoveTime = tune.getFloat("autom-double-armmove-time", 0.9f);
    private final FloatStatus doubleFireTime = tune.getFloat("autom-double-fire-time", 0.7f);
    private final FloatStatus doubleCollectTime = tune.getFloat("autom-double-collect-time", 0.9f);
    private final FloatStatus doubleAlignTime1 = tune.getFloat("autom-double-align1-time", 0.4f);
    private final FloatStatus doubleAlignTime2 = tune.getFloat("autom-double-align2-time", 0.5f);

    private void autoDouble() throws InterruptedException, AutonomousModeOverException {
        Logger.fine("Began double mode!");
        useCurrent.writeValue(true);
        winchGotten.writeValue(false);
        rearmWhenEvent.produce();
        if (doubleAlignTime1.readValue() > 0.02) {
            bothDrive.writeValue(-1);
            Logger.fine("Aligning...");
            waitForTime(doubleAlignTime1);
            Logger.fine("Aligned.");
            bothDrive.writeValue(0);
        }
        Logger.info("Collect On");
        collect.writeValue(1f);
        alignArm.eventFired();
        waitForTime((long) (1000L * doubleArmMoveTime.readValue() + 0.5f) / 2);
        Logger.info("Collect Off");
        collect.writeValue(0f);
        waitUntil(winchGotten);
        waitForTime((long) (1000L * doubleArmMoveTime.readValue() + 0.5f) / 2);
        useCurrent.writeValue(false);
        Logger.fine("Arm moved - firing!");
        fireWhenEvent.produce();
        waitForTime(doubleFireTime);
        winchGotten.writeValue(false);
        lowerArm.eventFired();
        rearmWhenEvent.produce();
        collectSols.writeValue(true);
        Logger.fine("Rearming... (and driving)");
        bothDrive.writeValue(1f);
        collect.writeValue(0.5f);
        waitForTime(doubleAlignTime2);
        Logger.fine("Drove!");
        bothDrive.writeValue(0f);
        waitUntil(winchGotten);
        collect.writeValue(0f);
        Logger.fine("Rearmed!");
        collect.writeValue(1f);
        waitForTime(doubleCollectTime);
        alignArm.eventFired();
        collectSols.writeValue(false);
        collect.writeValue(0);
        Logger.fine("Collected.");
        if (doubleAlignTime2.readValue() > 0.02) {
            bothDrive.writeValue(-1);
            Logger.fine("Aligning...");
            waitForTime(doubleAlignTime2);
            Logger.fine("Aligned.");
            bothDrive.writeValue(0);
        }
        waitForTime(doubleArmMoveTime);
        Logger.fine("Firing...");
        fireWhenEvent.produce();
        waitForTime(doubleFireTime);
        raiseArm.eventFired();
        Logger.fine("Double completed.");
    }
    
    // *** Framework ***
    private void waitForTime(FloatInputPoll fin) throws InterruptedException, AutonomousModeOverException {
        waitForTime((long) (1000L * fin.readValue() + 0.5f));
    }

    public AutonomousController(InstinctRegistrar reg) {
        final EventConsumer reportAutonomous = new EventConsumer() {
            public void eventFired() {
                Logger.info("Autonomous mode is currently set to: " + option.get());
            }
        };
        reportAutonomous.eventFired();
        seg.attachStringHolder("autonomous-mode", option);
        CluckGlobals.getNode().publish("autom-check", reportAutonomous);
        CluckGlobals.getNode().publish("autom-next", new EventConsumer() {
            public void eventFired() {
                option.set(options[(optionList.indexOf(option.get()) + 1) % options.length]);
                reportAutonomous.eventFired();
            }
        });
        CluckGlobals.getNode().publish("autom-prev", new EventConsumer() {
            public void eventFired() {
                option.set(options[(optionList.indexOf(option.get()) - 1 + options.length) % options.length]);
                reportAutonomous.eventFired();
            }
        });
        final RemoteProcedure openDialog = CluckGlobals.getNode().getRPCManager().subscribe("phidget/display-dialog", 11000);
        CluckGlobals.getNode().publish("autom-select", new EventConsumer() {
            public void eventFired() {
                StringBuffer sb = new StringBuffer("TITLE Select Autonomous Mode\n");
                for (int i = 0; i < options.length; i++) {
                    sb.append("BUTTON ").append(options[i]).append('\n');
                }
                openDialog.invoke(sb.toString().getBytes(), new ByteArrayOutputStream() {
                    public void close() throws UnsupportedEncodingException {
                        String str = new String(this.toByteArray());
                        if (str.length() > 0 && optionList.indexOf(str) != -1) {
                            option.set(str);
                            reportAutonomous.eventFired();
                        }
                    }
                });
            }
        });
        register(reg);
    }

    public void putDriveMotors(FloatOutput leftDrive, FloatOutput rightDrive) {
        bothDrive = Mixing.combine(leftDrive, rightDrive);
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

    public void putArm(EventConsumer lowerArm, EventConsumer raiseArm, EventConsumer alignArm, FloatOutput collector, BooleanOutput collectorSolenoids) {
        this.lowerArm = lowerArm;
        this.raiseArm = raiseArm;
        this.alignArm = alignArm;
        collect = collector;
        collectSols = collectorSolenoids;
    }

    public EventConsumer getNotifyRearmFinished() {
        return winchGotten.getSetTrueEvent();
    }

    public void putCurrentActivator(BooleanStatus shouldUseCurrent) {
        useCurrent = shouldUseCurrent;
    }
}
