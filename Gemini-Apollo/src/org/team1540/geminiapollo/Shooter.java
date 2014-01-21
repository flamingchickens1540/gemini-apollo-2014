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
    */
    
    public static void createShooter(EventSource begin, EventSource during, final FloatOutput winchMotor, BooleanOutput winchEngageSolenoid, BooleanOutput winchReleaseSolenoid, FloatInputPoll winchCurrent, final BooleanInputPoll catapultCocked, EventSource rearmCatapult, EventSource fireButton) {
        Logger.warning("Shooter TOFINISH");
        //Network Variables
        TuningContext tuner = new TuningContext (CluckGlobals.node, "Shooter Values");
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
        
        //stop arming
        final EventConsumer stopArming = new EventConsumer () {
            public void eventFired () {
                running.writeValue(false);
            }
        };

        //begin
        running.setFalseWhen(begin);
        //and more!

        //during
        fireButton.addListener(new EventConsumer () {
            public void eventFired () {
                if (running.readValue()) {
                    running.writeValue(false);
                } else if (engaged.readValue()) {
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
                    if (catapultCocked.readValue()/*||magical winchcurrent check >= drawBack*/) {
                        running.writeValue(false);
                    }
                }
            }
        });
    }
}
