package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.ExpirationTimer;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.TuningContext;
import ccre.log.Logger;

public class Shooter {
    /* TODO LIST:
     -implement better stopping when winchcurrent reaches (almost reaches) set value
     -have a better stop for arming the catapult when arm is up (winchcurrent?)
     -get rid of log
     */

    public static BooleanInputPoll createShooter(
            final EventSource beginAutonomous, final EventSource beginTeleop, final EventSource during,
            final FloatOutput winchMotor,
            final BooleanOutput winchSolenoid, final BooleanOutput rachetLoopRelease,
            final FloatInputPoll winchCurrent,
            final BooleanInputPoll catapultNotCocked, final BooleanInputPoll armDown, final BooleanInputPoll detentioning,
            EventSource rearmCatapult, EventSource fireButton) {
        rachetLoopRelease.writeValue(false); // We switched the polarity.

        //Network Variables
        CluckGlobals.node.publish("Rachet", rachetLoopRelease);
        TuningContext tuner = new TuningContext(CluckGlobals.node, "ShooterValues");
        tuner.publishSavingEvent("Shooter");
        final FloatStatus winchSpeed = tuner.getFloat("Winch Speed", .3f);
        final FloatStatus drawBack = tuner.getFloat("Draw Back", 1.1f);
        final BooleanStatus shouldWinchDuringFire = new BooleanStatus(true);
        CluckGlobals.node.publish("Winch During Fire", shouldWinchDuringFire);

        //engage safety after firing safety
        final ExpirationTimer engageTimer = new ExpirationTimer();
        final BooleanStatus canEngage = new BooleanStatus(true);
        engageTimer.schedule(1, new EventConsumer() {
            public void eventFired() {
                Logger.info("After Fire Timer A");
                canEngage.writeValue(false);
            }
        });
        engageTimer.schedule(1000, new EventConsumer() {
            public void eventFired() {
                Logger.info("After Fire Timer B");
                canEngage.writeValue(true);
                engageTimer.stop();
            }
        });

        //run winch motor in reverse before firing
        final ExpirationTimer fireTimer = new ExpirationTimer();
        final BooleanStatus fireTimerRunning = new BooleanStatus();
        fireTimerRunning.writeValue(false);
        fireTimer.schedule(1, new EventConsumer() {
            public void eventFired() {
                fireTimerRunning.writeValue(true);
                Logger.info("During Fire Timer A");
            }
        });
        fireTimer.schedule(250, new EventConsumer() {
            public void eventFired() {
                fireTimerRunning.writeValue(false);
                Logger.info("During Fire Timer B");
                fireTimer.stop();
            }
        });

        //state of the catapult
        //four score, etc. etc.
        final BooleanStatus winchDisengaged = new BooleanStatus(winchSolenoid);
        final BooleanStatus rearming = new BooleanStatus();

        //begin listeners
        rearming.setFalseWhen(beginAutonomous);
        winchDisengaged.setFalseWhen(beginAutonomous);
        rearming.setFalseWhen(beginTeleop);
        beginTeleop.addListener(new EventConsumer() {
            public void eventFired() {
                if (canEngage.readValue()) {
                    winchDisengaged.writeValue(false);
                    //winchDisengaged.setFalseWhen(beginTeleop);
                }
            }
        });

        //Buttons
        fireButton.addListener(new EventConsumer() {
            public void eventFired() {
                if (rearming.readValue()) {
                    Logger.info("Fire A");
                    rearming.writeValue(false);
                } else if (!winchDisengaged.readValue() && armDown.readValue()) {
                    Logger.info("Fire B");
                    fireTimer.start();
                    winchDisengaged.writeValue(true);
                    engageTimer.start();
                } else {
                    Logger.info("Fire C");
                }
            }
        });
        rearmCatapult.addListener(new EventConsumer() {
            public void eventFired() {
                Logger.info("rearm");
                if (rearming.readValue()) {
                    Logger.info("stop rearm");
                    rearming.writeValue(false);
                } else if (armDown.readValue() && catapultNotCocked.readValue()) {
                    winchDisengaged.writeValue(false);
                    Logger.info("actually rearm");
                    rearming.writeValue(true);
                } else {
                    Logger.info("no rearm");
                }
            }
        });
        //during listener
        during.addListener(new EventConsumer() {
            public void eventFired() {
                if (rearming.readValue()) {
                    winchMotor.writeValue(winchSpeed.readValue());
                    if (!catapultNotCocked.readValue() || winchCurrent.readValue() >= drawBack.readValue()) {
                        rearming.writeValue(false);
                        winchMotor.writeValue(0f);
                    }
                } else if (fireTimerRunning.readValue() && shouldWinchDuringFire.readValue()) {
                    winchMotor.writeValue(1f);
                } else if (detentioning.readValue()) {
                    winchMotor.writeValue(-winchSpeed.readValue());
                } else {
                    winchMotor.writeValue(0f);
                }
            }
        });
        //this is for Gregor... I don't remeber what it's for but I think it is so he can't lift the arm when the catapult is rearming
        return Mixing.invert((BooleanInputPoll) rearming);
    }
}
