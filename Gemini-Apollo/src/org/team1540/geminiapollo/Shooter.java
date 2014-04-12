package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.*;
import ccre.event.*;
import ccre.holders.TuningContext;
import ccre.log.*;

public class Shooter {

    private final EventSource periodic, constantPeriodic;
    private final TuningContext tuner = new TuningContext(CluckGlobals.getNode(), "ShooterValues");
    public final BooleanStatus winchDisengaged = new BooleanStatus();
    public final BooleanStatus rearming = new BooleanStatus();
    private BooleanInputPoll winchPastThreshold;
    private final BooleanInputPoll isArmInTheWay;
    private final FloatInputPoll batteryLevel;
    private FloatInputPoll sensor;
    private EventConsumer lowerArm, guardedFire;
    public final FloatStatus totalPowerTaken = new FloatStatus();
    public final BooleanStatus shouldUseCurrent = new BooleanStatus();

    private final FloatInput winchSpeed = tuner.getFloat("Winch Speed", 1f);
    private final FloatInput drawBack = tuner.getFloat("Draw Back", 605);
    private final FloatInput drawBackExtra = tuner.getFloat("Extra Draw Back in Auto", 0);
    private final FloatInput drawBackCurrent = tuner.getFloat("Draw Back Current", 50);
    private final FloatInput rearmTimeout = tuner.getFloat("Winch Rearm Timeout", 5f);
    private final FloatInput ampThreshold = tuner.getFloat("Amp Threshold", 5f);

    public final FloatInputPoll activeAmps = new FloatInputPoll() {
        public float readValue() {
            if (sensor == null) {
                return -100; // TODO: Remove this later.
            }
            float o = (sensor.readValue() - 0.60f) / 0.04f;
            return o >= ampThreshold.readValue() ? o : 0;
        }
    };

    private final FloatInputPoll activeWatts = new FloatInputPoll() {
        public float readValue() {
            return activeAmps.readValue() * batteryLevel.readValue();
        }
    };

    private final EventConsumer updateTotal = new EventConsumer() {
        public void eventFired() {
            totalPowerTaken.writeValue(totalPowerTaken.readValue() + activeWatts.readValue() / 100);
        }
    };

    public Shooter(EventSource resetModule, EventSource periodic, EventSource constantPeriodic, final BooleanInputPoll isArmNotInTheWay, final FloatInputPoll batteryLevel) {
        this.periodic = periodic;
        this.constantPeriodic = constantPeriodic;
        constantPeriodic.addListener(updateTotal); // TODO Move this after the shooter is registered.
        winchDisengaged.setFalseWhen(resetModule);
        rearming.setFalseWhen(resetModule);
        tuner.publishSavingEvent("Shooter");
        this.isArmInTheWay = Mixing.invert(isArmNotInTheWay);
        this.batteryLevel = batteryLevel;
        CluckGlobals.getNode().publish("Constant", constantPeriodic);
        CluckGlobals.getNode().publish("ActiveAmps", Mixing.createDispatch(activeAmps, constantPeriodic));
        CluckGlobals.getNode().publish("ActiveWatts", Mixing.createDispatch(activeWatts, constantPeriodic));
        CluckGlobals.getNode().publish("TotalWatts", totalPowerTaken);
        CluckGlobals.getNode().publish("ShouldUseCurrent", shouldUseCurrent);
    }

    public void setupArmLower(EventConsumer enc, BooleanOutput runCollector) {
        ExpirationTimer fireAfterLower = new ExpirationTimer();
        fireAfterLower.schedule(50, enc);
        fireAfterLower.scheduleBooleanPeriod(40, 1100, runCollector, true);
        if (this.guardedFire == null || enc == null) {
            throw new NullPointerException();
        }
        fireAfterLower.schedule(1200, this.guardedFire);
        fireAfterLower.schedule(1300, fireAfterLower.getStopEvent());
        lowerArm = fireAfterLower.getStartEvent();
    }

    public void setupWinch(final FloatOutput winchMotor, final BooleanOutput winchSolenoid,
            final FloatInputPoll winchCurrent, final BooleanInputPoll isAutonomous) {
        sensor = winchCurrent;
        winchDisengaged.addTarget(winchSolenoid);
        CluckGlobals.getNode().publish("Winch Disengaged", winchDisengaged);
        winchPastThreshold = new BooleanInputPoll() {
            public boolean readValue() {
                return shouldUseCurrent.readValue() ? activeAmps.readValue() >= drawBackCurrent.readValue() : totalPowerTaken.readValue() >= (isAutonomous.readValue() ? drawBackExtra.readValue() : 0) + drawBack.readValue();
            }
        };
        rearming.addTarget(Mixing.select(winchMotor, Mixing.always(0), winchSpeed));
    }

    public void setupRearmTimeout() {
        final FloatStatus resetRearm = new FloatStatus();
        resetRearm.setWhen(0, Mixing.whenBooleanBecomes(rearming, false));
        CluckGlobals.getNode().publish("Winch Rearm Timeout Status", (FloatInput) resetRearm);
        constantPeriodic.addListener(new EventConsumer() {
            public void eventFired() {
                float val = resetRearm.readValue();
                if (val > 0) {
                    val -= 0.01f;
                    if (val <= 0 && rearming.readValue()) {
                        Logger.info("Rearm Timeout");
                        rearming.writeValue(false);
                    }
                    resetRearm.writeValue(val);
                } else if (rearming.readValue()) {
                    resetRearm.writeValue(rearmTimeout.readValue());
                }
            }
        });
    }

    private void autolowerArm() {
        Logger.info("Autolower!");
        lowerArm.eventFired();
    }

    public void handleShooterButtons(
            final EventSource rearmTrigger, EventSource fireButton, final EventConsumer finishedRearm) {
        final EventConsumer realFire = Mixing.combine(
                new EventLogger(LogLevel.INFO, "Fire Begin"),
                winchDisengaged.getSetTrueEvent());
        CluckGlobals.getNode().publish("Force Fire", realFire);
        fireButton.addListener(this.guardedFire = new EventConsumer() {
            public void eventFired() {
                if (rearming.readValue()) {
                    Logger.info("fire button: stop rearm");
                    rearming.writeValue(false);
                    ErrorMessages.displayError(5, "Cancelled rearm.", 1000);
                } else if (winchDisengaged.readValue()) {
                    Logger.info("no fire: run the winch!");
                    ErrorMessages.displayError(3, "Winch not armed.", 2000);
                } else if (isArmInTheWay.readValue()) {
                    Logger.info("no fire: autolowering the arm.");
                    ErrorMessages.displayError(4, "Autolowering arm.", 1000);
                    autolowerArm();
                } else {
                    realFire.eventFired();
                    ErrorMessages.displayError(1, "Firing", 500);
                }
            }
        });
        rearmTrigger.addListener(new EventConsumer() {
            public void eventFired() {
                if (rearming.readValue()) {
                    Logger.info("stop rearm");
                    rearming.writeValue(false);
                    ErrorMessages.displayError(5, "Cancelled rearm.", 1000);
                } else if (isArmInTheWay.readValue()) {
                    Logger.info("no rearm: lower the arm!");
                    ErrorMessages.displayError(4, "Arm isn't down.", 500);
                } else {
                    winchDisengaged.writeValue(false);
                    Logger.info("rearm");
                    ErrorMessages.displayError(1, "Started rearming.", 500);
                    rearming.writeValue(true);
                    totalPowerTaken.writeValue(0);
                }
            }
        });
        periodic.addListener(new EventConsumer() {
            public void eventFired() {
                if (rearming.readValue() && winchPastThreshold.readValue()) {
                    rearming.writeValue(false);
                    Logger.info("drawback current stop rearm");
                    ErrorMessages.displayError(2, "Hit current limit.", 1000);
                    finishedRearm.eventFired();
                }
            }
        });
    }
}
