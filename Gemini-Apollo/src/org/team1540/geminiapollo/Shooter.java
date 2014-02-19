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

    public static void createShooter(
            final EventSource beginAutonomous, final EventSource beginTeleop, final EventSource during,
            final FloatOutput winchMotor,
            final BooleanOutput winchSolenoid, final BooleanOutput rachetLoopRelease,
            final FloatInputPoll winchCurrent, final FloatInputPoll slider,
            final BooleanInputPoll catapultNotCocked, final BooleanInputPoll armDown, final BooleanInputPoll detentioning,
            EventSource rearmCatapult, EventSource fireButton) {
        rachetLoopRelease.writeValue(false); // We switched the polarity.

        //Network Variables
        CluckGlobals.node.publish("Rachet", rachetLoopRelease);
        TuningContext tuner = new TuningContext(CluckGlobals.node, "ShooterValues");
        tuner.publishSavingEvent("Shooter");
        final FloatStatus winchSpeed = tuner.getFloat("Winch Speed", 1f);
        final FloatStatus drawBack = tuner.getFloat("Draw Back", 6f);
        final FloatStatus currentMinAdjustor = tuner.getFloat("Minimum Current Adjustor", 0f);
        final FloatStatus currentMultiplierAdjustor = tuner.getFloat("Multiplier Current Adjustor", 5f);
        final BooleanStatus shouldWinchDuringFire = new BooleanStatus(true);
        CluckGlobals.node.publish("Winch During Fire", shouldWinchDuringFire);
        final BooleanStatus useSlider = new BooleanStatus(true);
        CluckGlobals.node.publish("Use Slider Drawback Value", useSlider);
        CluckGlobals.node.publish("Slider Value", Mixing.createDispatch(slider, during));

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
        
         //run winch motor in reverse to reduce tension
         final ExpirationTimer reduceTensionTimer = new ExpirationTimer();
         final BooleanStatus reduceTensionTimerRunning = new BooleanStatus();
         reduceTensionTimerRunning.writeValue(false);
         reduceTensionTimer.schedule(1, new EventConsumer() {
         public void eventFired() {
         reduceTensionTimerRunning.writeValue(true);
         Logger.info("Reduce Tension Begin");
         }
         });
         reduceTensionTimer.schedule(250, new EventConsumer() {
         public void eventFired() {
         reduceTensionTimerRunning.writeValue(false);
         Logger.info("Reduce Tension End");
         reduceTensionTimer.stop();
         }
         });
  
        //state of the catapult
        //four score, etc. etc.
        //detentioning is technically a part of this
        final BooleanStatus winchDisengaged = new BooleanStatus(winchSolenoid);
        final BooleanStatus rearming = new BooleanStatus();
        final FloatInputPoll adjustedSlider = new FloatInputPoll() {

            public float readValue() {
                return ((slider.readValue() + currentMinAdjustor.readValue()) * currentMultiplierAdjustor.readValue());
            }
        };
        CluckGlobals.node.publish("Adjusted Slider Value", Mixing.createDispatch(adjustedSlider, during));

        //detentioning
        Mixing.whenBooleanBecomes(detentioning, true, during).addListener(new EventConsumer() {
            public void eventFired() {
                rearming.writeValue(false);
                reduceTensionTimer.start();
                rachetLoopRelease.writeValue(true);
            }
        });
        Mixing.whenBooleanBecomes(detentioning, false, during).addListener(new EventConsumer() {
            public void eventFired() {
                rachetLoopRelease.writeValue(false);
            }
        });
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
                    Logger.info("fire- stop rearm");
                    rearming.writeValue(false);
                } else if (!winchDisengaged.readValue() && armDown.readValue()) {
                    Logger.info("fire");
                    winchDisengaged.writeValue(true);
                    engageTimer.start();
                } else {
                    Logger.info("no fire");
                }
            }
        });
        rearmCatapult.addListener(new EventConsumer() {
            public void eventFired() {
                if (rearming.readValue()) {
                    Logger.info("stop rearm");
                    rearming.writeValue(false);
                } else if (catapultNotCocked.readValue()) {
                    winchDisengaged.writeValue(false);
                    Logger.info("rearm");
                    rachetLoopRelease.writeValue(false);
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
                    if (!catapultNotCocked.readValue()) {
                        rearming.writeValue(false);
                        winchMotor.writeValue(0f);
                        Logger.info("limit switch stop rearm");
                    } else if (useSlider.readValue() && winchCurrent.readValue() >= adjustedSlider.readValue()) {
                        rearming.writeValue(false);
                        winchMotor.writeValue(0f);
                        Logger.info("slider drawback current stop rearm");
                    } else if (!useSlider.readValue() && winchCurrent.readValue() >= drawBack.readValue()) {
                        rearming.writeValue(false);
                        winchMotor.writeValue(0f);
                        Logger.info("manual drawback current stop rearm");
                    }
                    } else if (reduceTensionTimerRunning.readValue() && shouldWinchDuringFire.readValue()) {
                     winchMotor.writeValue(winchSpeed.readValue());
                } else if (detentioning.readValue()) {
                    winchMotor.writeValue(-winchSpeed.readValue());
                } else {
                    winchMotor.writeValue(0f);
                }
            }
        });
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
