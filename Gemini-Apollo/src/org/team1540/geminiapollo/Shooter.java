package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.ExpirationTimer;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.TuningContext;
import ccre.log.LogLevel;
import ccre.log.Logger;

public class Shooter {

    public static BooleanInputPoll createShooter(
            final EventSource beginAutonomous, final EventSource beginTeleop, final EventSource during, final EventSource constant,
            final BooleanInputPoll isautonomous,
            final FloatOutput winchMotor,
            final BooleanOutput winchSolenoid,
            final FloatInputPoll winchCurrent,
            final BooleanInputPoll catapultNotCocked, final BooleanInputPoll armDown,
            final BooleanInput rearmCatapult, final EventSource rearmAutonomousTrigger, EventSource fireButton, BooleanOutput canCollectorRun,
            final BooleanStatus winchDisengaged, final EventConsumer finishedRearm) {
        //Network Variables
        TuningContext tuner = new TuningContext(CluckGlobals.node, "ShooterValues");
        tuner.publishSavingEvent("Shooter");
        final FloatStatus winchSpeed = tuner.getFloat("Winch Speed", 1f);
        final FloatStatus drawBack = tuner.getFloat("Draw Back", 6f);
        final FloatStatus rearmTimeout = tuner.getFloat("Winch Rearm Timeout", 5f);
        //engage safety after firing safety
        final ExpirationTimer engageTimer = new ExpirationTimer();
        final BooleanStatus cannotEngage = new BooleanStatus(engageTimer.getRunningControl());
        engageTimer.schedule(1000, new EventConsumer() {
            public void eventFired() {
                Logger.info("Fire complete.");
                cannotEngage.writeValue(false);
            }
        });
        //state of the catapult
        //four score, etc. etc.
        //detentioning is technically a part of this
        CluckGlobals.node.publish("Winch Disengaged", winchDisengaged);
        winchDisengaged.addTarget(Mixing.invert(canCollectorRun));
        final BooleanStatus rearming = new BooleanStatus();
        //timeout on rearming
        final FloatStatus resetRearm = new FloatStatus();
        resetRearm.setWhen(0, Mixing.whenBooleanBecomes(rearming, false));
        CluckGlobals.node.publish("Winch Rearm Timeout Status", (FloatInput) resetRearm);
        constant.addListener(new EventConsumer() {
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
        //begin listeners
        rearming.setFalseWhen(beginAutonomous);
        winchDisengaged.setFalseWhen(beginAutonomous);
        rearming.setFalseWhen(beginTeleop);
        winchDisengaged.setFalseWhen(beginTeleop);
        //Buttons
        final EventConsumer realFire = new EventConsumer() {
            public void eventFired() {
                Logger.info("fire");
                winchDisengaged.writeValue(true);
                cannotEngage.writeValue(true);
            }
        };
        CluckGlobals.node.publish("Force Fire", realFire);
        fireButton.addListener(new EventConsumer() {
            public void eventFired() {
                if (rearming.readValue()) {
                    Logger.info("fire button: stop rearm");
                    rearming.writeValue(false);
                    ControlInterface.displayError("Cancelled rearm.");
                } else if (winchDisengaged.readValue()) {
                    Logger.info("no fire: run the winch!");
                    ControlInterface.displayError("Winch not armed.");
                } else if (armDown.readValue() || isautonomous.readValue()) {
                    realFire.eventFired();
                } else {
                    Logger.info("no fire: lower the arm!");
                    ControlInterface.displayError("Arm isn't down.");
                }
            }
        });
        EventConsumer doRearm = new EventConsumer() {
            public void eventFired() {
                if (rearming.readValue()) {
                    Logger.info("stop rearm");
                    rearming.writeValue(false);
                    ControlInterface.displayError("Cancelled rearm.");
                } else if (catapultNotCocked.readValue()) {
                    winchDisengaged.writeValue(false);
                    Logger.info("rearm");
                    rearming.writeValue(true);
                } else {
                    Logger.info("no rearm");
                    ControlInterface.displayError("Already at limit.");
                }
            }
        };
        rearmAutonomousTrigger.addListener(doRearm);
        Mixing.whenBooleanBecomes(rearmCatapult, true).addListener(doRearm);
        //during listener
        during.addListener(new EventConsumer() {
            public void eventFired() {
                if (rearming.readValue() && (!catapultNotCocked.readValue() || winchCurrent.readValue() >= drawBack.readValue())) {
                    rearming.writeValue(false);
                    winchMotor.writeValue(0f);
                    Logger.info(catapultNotCocked.readValue() ? "drawback current stop rearm" : "limit switch stop rearm");
                    finishedRearm.eventFired();
                    return;
                }
                winchMotor.writeValue((rearming.readValue() || rearmCatapult.readValue()) ? winchSpeed.readValue() : 0);
            }
        });
        return Mixing.invert((BooleanInputPoll) rearming);
    }

    public static void createTuner(EventSource during, final FloatInputPoll sensor, EventSource rearmCatapult, final BooleanInputPoll catapultNotCocked) {
        final FloatStatus active = new FloatStatus(-1);
        final BooleanStatus enabled = new BooleanStatus();
        enabled.setTrueWhen(rearmCatapult);
        active.setWhen(0, rearmCatapult);
        during.addListener(new EventConsumer() {
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
