package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.ExpirationTimer;
import ccre.ctrl.Mixing;
import ccre.ctrl.MultipleSourceBooleanController;
import ccre.event.*;
import ccre.holders.TuningContext;
import ccre.log.LogLevel;
import ccre.log.Logger;

public class Shooter {

    private final EventSource globalPeriodic, constantPeriodic;
    private final TuningContext tuner = new TuningContext(CluckGlobals.node, "ShooterValues");
    public final BooleanStatus winchDisengaged = new BooleanStatus();
    public final BooleanStatus rearming = new BooleanStatus();
    private BooleanInputPoll winchPastThreshold;
    private final BooleanInputPoll isArmInTheWay;
    private EventConsumer lowerArm, guardedFire;

    private final FloatInput winchSpeed = tuner.getFloat("Winch Speed", 1f);
    private final FloatInput drawBack = tuner.getFloat("Draw Back", 2.5f);
    private final FloatInput rearmTimeout = tuner.getFloat("Winch Rearm Timeout", 5f);

    public Shooter(EventSource resetModule, EventSource globalPeriodic, EventSource constantPeriodic, final BooleanInputPoll isArmNotInTheWay) {
        this.globalPeriodic = globalPeriodic;
        this.constantPeriodic = constantPeriodic;
        winchDisengaged.setFalseWhen(resetModule);
        rearming.setFalseWhen(resetModule);
        tuner.publishSavingEvent("Shooter");
        this.isArmInTheWay = Mixing.invert(isArmNotInTheWay);
    }
    
    public void setupArmLower(EventConsumer enc) {
        ExpirationTimer fireAfterLower = new ExpirationTimer();
        fireAfterLower.schedule(50, enc);
        if (this.guardedFire == null || enc == null) {
            throw new NullPointerException();
        }
        fireAfterLower.schedule(1000, this.guardedFire);
        fireAfterLower.schedule(1100, fireAfterLower.getStopEvent());
        lowerArm = fireAfterLower.getStartEvent();
    }

    public void setupWinch(final FloatOutput winchMotor, final BooleanOutput winchSolenoid,
            final FloatInputPoll winchCurrent, final BooleanInput forceRearm) {
        winchDisengaged.addTarget(winchSolenoid);
        CluckGlobals.node.publish("Winch Disengaged", winchDisengaged);
        winchPastThreshold = new BooleanInputPoll() {
            public boolean readValue() {
                return winchCurrent.readValue() >= drawBack.readValue();
            }
        };
        MultipleSourceBooleanController runWinch = new MultipleSourceBooleanController(MultipleSourceBooleanController.OR);
        runWinch.addInput(rearming);
        runWinch.addInput(Mixing.andBooleans(forceRearm, Mixing.invert(isArmInTheWay)));
        runWinch.addTarget(Mixing.select(winchMotor, Mixing.always(0), winchSpeed));
    }

    public void setupRearmTimeout() {
        final FloatStatus resetRearm = new FloatStatus();
        resetRearm.setWhen(0, Mixing.whenBooleanBecomes(rearming, false));
        CluckGlobals.node.publish("Winch Rearm Timeout Status", (FloatInput) resetRearm);
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

    public void handleShooterButtons(final BooleanInputPoll catapultCocked,
            final EventSource rearmTrigger, EventSource fireButton, final EventConsumer finishedRearm) {
        final EventConsumer realFire = Mixing.combine(
                new EventLogger(LogLevel.INFO, "Fire Begin"),
                winchDisengaged.getSetTrueEvent());
        CluckGlobals.node.publish("Force Fire", realFire);
        fireButton.addListener(this.guardedFire = new EventConsumer() {
            public void eventFired() {
                if (rearming.readValue()) {
                    Logger.info("fire button: stop rearm");
                    rearming.writeValue(false);
                    ControlInterface.displayError("Cancelled rearm.");
                } else if (winchDisengaged.readValue()) {
                    Logger.info("no fire: run the winch!");
                    ControlInterface.displayError("Winch not armed.");
                } else if (isArmInTheWay.readValue()) {
                    Logger.info("no fire: autolowering the arm.");
                    ControlInterface.displayError("Autolowering arm.");
                    autolowerArm();
                } else {
                    realFire.eventFired();
                }
            }
        });
        rearmTrigger.addListener(new EventConsumer() {
            public void eventFired() {
                if (rearming.readValue()) {
                    Logger.info("stop rearm");
                    rearming.writeValue(false);
                    ControlInterface.displayError("Cancelled rearm.");
                } else if (catapultCocked.readValue()) {
                    Logger.info("no rearm");
                    ControlInterface.displayError("Already at limit.");
                } else if (isArmInTheWay.readValue()) {
                    Logger.info("no rearm: lower the arm!");
                    ControlInterface.displayError("Arm isn't down.");
                } else {
                    winchDisengaged.writeValue(false);
                    Logger.info("rearm");
                    rearming.writeValue(true);
                }
            }
        });
        globalPeriodic.addListener(new EventConsumer() {
            public void eventFired() {
                if (rearming.readValue() && (catapultCocked.readValue() || winchPastThreshold.readValue())) {
                    rearming.writeValue(false);
                    Logger.info(catapultCocked.readValue() ? "limit switch stop rearm" : "drawback current stop rearm");
                    finishedRearm.eventFired();
                }
            }
        });
    }

    public void createTuner(final FloatInputPoll sensor, EventSource rearmCatapult, final BooleanInputPoll catapultNotCocked) {
        final FloatStatus active = new FloatStatus(-1);
        final BooleanStatus enabled = new BooleanStatus();
        enabled.setTrueWhen(rearmCatapult);
        active.setWhen(0, rearmCatapult);
        globalPeriodic.addListener(new EventConsumer() {
            public void eventFired() {
                if (enabled.readValue()) {
                    float sense = sensor.readValue();
                    if (sense > active.readValue()) {
                        active.writeValue(sense);
                    }
                    if (!catapultNotCocked.readValue()) {
                        enabled.writeValue(false);
                    }
                }
            }
        });
        CluckGlobals.node.publish("Winch Max Current", (FloatInput) active);
        CluckGlobals.node.publish("Winch Max Enabled", (BooleanInput) enabled);
    }
}
