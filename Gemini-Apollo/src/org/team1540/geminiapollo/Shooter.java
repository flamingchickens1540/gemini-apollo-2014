package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.chan.BooleanOutput;
import ccre.chan.BooleanStatus;
import ccre.chan.FloatInputPoll;
import ccre.chan.FloatOutput;
import ccre.chan.FloatStatus;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.ExpirationTimer;
import ccre.ctrl.Mixing;
import ccre.event.EventConsumer;
import ccre.event.EventSource;
import ccre.holders.TuningContext;
import ccre.log.Logger;

public class Shooter {
    /* TODO LIST:
     begin - find out what the status is at the beginning of the match
     - find out wich value of armstatus is arm down
     -implement better stopping when winchcurrent reaches (almost reaches) set value
     -have a better stop for arming the catapult when arm is up (winchcurrent?)
     */

    public static BooleanInputPoll createShooter(EventSource beginAutonomous, final EventSource beginTeleop, EventSource during, final FloatOutput winchMotor, BooleanOutput winchSolenoid, final FloatInputPoll winchCurrent, final BooleanInputPoll catapultNotCocked, EventSource rearmCatapult, EventSource fireButton, final BooleanInputPoll armDown, BooleanOutput rachetLoopRelease) {
        Logger.warning("Shooter TOFINISH");
        Logger.warning("Catapult/arm collision software-stop not implemented yet.");
        //Network Variables
        CluckGlobals.node.publish("DEBUG rachet-loop", rachetLoopRelease);
        TuningContext tuner = new TuningContext(CluckGlobals.node, "ShooterValues");
        tuner.publishSavingEvent("Shooter");
        final FloatStatus winchSpeed = tuner.getFloat("Winch Speed", .3f);
        final FloatStatus drawBack = tuner.getFloat("Draw Back", 1.1f);

        //rearm safety
        final ExpirationTimer engageTimer = new ExpirationTimer();
        final BooleanStatus canEngage = new BooleanStatus();
        CluckGlobals.node.publish("DEBUG CanEngage", canEngage);
        canEngage.writeValue(true);
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
        
        //ease pressure on winch solenoid before firing
        final ExpirationTimer fireTimer = new ExpirationTimer ();
        fireTimer.schedule(1, new EventConsumer () {
            public void eventFired () {
                Logger.info("During Fire Timer A");
                winchMotor.writeValue(-1f);
            }
        });
        fireTimer.schedule(100, new EventConsumer () {
            public void eventFired () {
                winchMotor.writeValue(0f);
                Logger.info("During Fire Timer B");
                fireTimer.stop();
            }
        });

        //state of the catapult
        //four score, etc. etc.
        final BooleanStatus winchDisengaged = new BooleanStatus(winchSolenoid);
        CluckGlobals.node.publish("DEBUG WinchDisengaged", winchDisengaged);
        final BooleanStatus running = new BooleanStatus();
        CluckGlobals.node.publish("DEBUG running", running);

        //begin
        running.setFalseWhen(beginAutonomous);
        winchDisengaged.setFalseWhen(beginAutonomous);
        running.setFalseWhen(beginTeleop);
        beginTeleop.addListener(new EventConsumer() {
            public void eventFired() {
                if (canEngage.readValue()) {
                    winchDisengaged.writeValue(false);
                    //winchDisengaged.setFalseWhen(beginTeleop);
                }
            }
        });

        //and more!
        //during
        fireButton.addListener(new EventConsumer() {
            public void eventFired() {
                if (running.readValue()) {
                    Logger.info("Fire A");
                    running.writeValue(false);
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
                if (running.readValue()) {
                    Logger.info("stop rearm");
                    running.writeValue(false);
                } else if (armDown.readValue() && catapultNotCocked.readValue()) {
                    winchDisengaged.writeValue(false);
                    Logger.info("actually rearm");
                    running.writeValue(true);
                } else {
                    Logger.info("else rearm");
                }
            }
        });
        during.addListener(new EventConsumer() {
            public void eventFired() {
                if (running.readValue()) {
                    winchMotor.writeValue(winchSpeed.readValue());
                    if (!catapultNotCocked.readValue() || winchCurrent.readValue() >= drawBack.readValue()) {
                        running.writeValue(false);
                        winchMotor.writeValue(0f);
                    }
                } else {
                    winchMotor.writeValue(0f);
                }
            }
        });
        return Mixing.invert((BooleanInputPoll) running);
    }
}
