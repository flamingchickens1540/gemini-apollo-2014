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

    public static void createShooter(EventSource beginAutonomous, EventSource beginTeleop, EventSource during, final FloatOutput winchMotor, BooleanOutput winchSolenoid, final FloatInputPoll winchCurrent, final BooleanInputPoll catapultCocked, EventSource rearmCatapult, EventSource fireButton, final BooleanInputPoll armStatus, BooleanOutput rachetLoopRelease) {
        Logger.warning("Shooter TOFINISH");
        Logger.warning("Catapult/arm collision software-stop not implemented yet.");
        //Network Variables
        TuningContext tuner = new TuningContext(CluckGlobals.node, "ShooterValues");
        tuner.publishSavingEvent("Shooter");
        final FloatStatus winchSpeed = tuner.getFloat("Winch Speed", .25f);
        final FloatStatus drawBack = tuner.getFloat("Draw Back", 1f);
        
        //rearm safety
        final ExpirationTimer timer = new ExpirationTimer ();
        final BooleanStatus canEngage = new BooleanStatus ();
        canEngage.writeValue(true);
        timer.schedule(1, new EventConsumer () {
            public void eventFired () {
                canEngage.writeValue(false);
            }
        });
        timer.schedule(1000, new EventConsumer () {
            public void eventFired () {
                canEngage.writeValue(true);
                timer.stop();
            }
        });
        
        //state of the catapult
        //four score, etc. etc.
        final BooleanStatus winchDisengaged = new BooleanStatus(winchSolenoid);
        final BooleanStatus running = new BooleanStatus();

        //begin
        running.setFalseWhen(beginAutonomous);
        winchDisengaged.setFalseWhen(beginAutonomous);
        running.setFalseWhen(beginTeleop);
        if (canEngage.readValue()) {
            winchDisengaged.setFalseWhen(beginAutonomous);
        }

        //and more!
        //during
        fireButton.addListener(new EventConsumer() {
            public void eventFired() {
                if (running.readValue()) {
                    running.writeValue(false);
                } else if (!winchDisengaged.readValue() && !armStatus.readValue()) {
                    winchDisengaged.writeValue(true);
                    timer.start();
                }
            }
        });
        rearmCatapult.addListener(new EventConsumer() {
            public void eventFired() {
                if (running.readValue()) {
                    running.writeValue(false);
                } else if (!armStatus.readValue() && !catapultCocked.readValue()) {
                    winchDisengaged.writeValue(false);
                    running.writeValue(true);
                }
            }
        });
        during.addListener(new EventConsumer() {
            public void eventFired() {
                if (running.readValue()) {
                    winchMotor.writeValue(winchSpeed.readValue());
                    if (catapultCocked.readValue() || winchCurrent.readValue() >= drawBack.readValue()) {
                        running.writeValue(false);
                        winchMotor.writeValue(0f);
                    }
                } else {
                    winchMotor.writeValue(0f);
                }
            }
        });
    }
}
