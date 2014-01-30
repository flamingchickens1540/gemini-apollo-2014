package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.chan.BooleanOutput;
import ccre.chan.BooleanStatus;
import ccre.chan.FloatInputPoll;
import ccre.chan.FloatOutput;
import ccre.chan.FloatStatus;
import ccre.cluck.CluckGlobals;
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
    public static void createShooter(EventSource begin, EventSource during, final FloatOutput winchMotor, BooleanOutput winchEngageSolenoid, BooleanOutput winchReleaseSolenoid, FloatInputPoll winchCurrent, final BooleanInputPoll catapultCocked, EventSource rearmCatapult, EventSource fireButton, final BooleanInputPoll armStatus) {
        Logger.warning("Shooter TOFINISH");
        Logger.warning("Catapult/arm collision software-stop not implemented yet.");
        //Network Variables
        TuningContext tuner = new TuningContext (CluckGlobals.node, "ShooterValues");
        tuner.publishSavingEvent("Shooter");
        final FloatStatus winchSpeed = tuner.getFloat("Winch Speed", .25f);
        final FloatStatus drawBack = tuner.getFloat("Draw Back", 1f);
        
        //state of the catapult
        //four score, etc. etc.
        final BooleanStatus engaged = new BooleanStatus(winchEngageSolenoid);
        final BooleanStatus disengaged = new BooleanStatus(winchReleaseSolenoid);
        engaged.setFalseWhen(Mixing.whenBooleanBecomes(disengaged, true));
        disengaged.setFalseWhen(Mixing.whenBooleanBecomes(engaged, true));
        final BooleanStatus running = new BooleanStatus();
        
        //begin
        running.setFalseWhen(begin);
        
        //and more!

        //during
        fireButton.addListener(new EventConsumer () {
            public void eventFired () {
                if (running.readValue()) {
                    running.writeValue(false);
                } else if (engaged.readValue() && armStatus.readValue() == false) {
                    disengaged.writeValue(true);
                }
            }
        });
        rearmCatapult.addListener(new EventConsumer () {
            public void eventFired () {
                if (running.readValue()) {
                    running.writeValue(false);
                } else {
                    engaged.writeValue(true);
                    running.writeValue(true);
                }
            }
        });
        during.addListener(new EventConsumer() {
            public void eventFired() {
                if (running.readValue()) {
                    winchMotor.writeValue(winchSpeed.readValue());
                    if (catapultCocked.readValue() || armStatus.readValue()/*||magical winchcurrent check >= drawBack*/) {
                        running.writeValue(false);
                    }
                } else {
                    winchMotor.writeValue(0f);
                }
            }
        });
    }
}
