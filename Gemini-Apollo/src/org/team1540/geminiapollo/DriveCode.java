package org.team1540.geminiapollo;

import ccre.chan.BooleanOutput;
import ccre.chan.BooleanStatus;
import ccre.chan.FloatFilter;
import ccre.chan.FloatInputPoll;
import ccre.chan.FloatOutput;
import ccre.chan.FloatStatus;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.Mixing;
import ccre.event.EventConsumer;
import ccre.event.EventSource;
import ccre.holders.TuningContext;
import ccre.log.Logger;

public class DriveCode {

    /*TO DO:
     -add motor adjustors...
     -during for createShifting is currently unnecessary, might remove
     */
    public static void createDrive(EventSource begin, EventSource during, final FloatOutput leftDrive1, final FloatOutput leftDrive2, final FloatOutput rightDrive1, final FloatOutput rightDrive2, FloatInputPoll leftDriveAxis, FloatInputPoll rightDriveAxis, FloatInputPoll forwardDriveAxis, final boolean competitionRobot) {
        //Gemini tuning
        TuningContext gWheelTuner = new TuningContext(CluckGlobals.node, "Gemini Wheel Values");
        gWheelTuner.publishSavingEvent("Gemini Wheel Save");
        final FloatStatus gfLeft = gWheelTuner.getFloat("Gemini Left Forwards", 624 / 691f);
        final FloatStatus gbLeft = gWheelTuner.getFloat("Gemini Left Backwards", 613 / 704f);
        final FloatStatus gfRight = gWheelTuner.getFloat("Gemini Right Forwards", 1f);
        final FloatStatus gbRight = gWheelTuner.getFloat("Gemini Right Backwards", 1f);

        //Apollo tuning
        TuningContext aWheelTuner = new TuningContext(CluckGlobals.node, "Apollo Wheel Values");
        aWheelTuner.publishSavingEvent("Apollo Wheel Save");
        final FloatStatus afLeft = aWheelTuner.getFloat("Apollo Left Forwards", 1f);
        final FloatStatus abLeft = aWheelTuner.getFloat("Apollo Left Backwards", 1f);
        final FloatStatus afRight = aWheelTuner.getFloat("Apollo Right Forwards", 1f);
        final FloatStatus abRight = aWheelTuner.getFloat("Apollo Right Backwards", 1f);

        //dead zone
        FloatFilter deadZone = Mixing.deadzone(.1f);
        final FloatInputPoll leftDriveAxisW = deadZone.wrap(leftDriveAxis);
        final FloatInputPoll rightDriveAxisW = deadZone.wrap(rightDriveAxis);
        final FloatInputPoll forwardDriveAxisW = deadZone.wrap(forwardDriveAxis);

        //begin
        begin.addListener(new EventConsumer() {
            public void eventFired() {
                leftDrive1.writeValue(0);
                leftDrive2.writeValue(0);
                rightDrive1.writeValue(0);
                rightDrive2.writeValue(0);
            }
        });

        //during
        during.addListener(new EventConsumer() {
            public void eventFired() {
                //motor values
                float leftDriveValue = (leftDriveAxisW.readValue() + forwardDriveAxisW.readValue());
                float rightDriveValue = (rightDriveAxisW.readValue() + forwardDriveAxisW.readValue());

                //adjust motor values
                if (competitionRobot) {
                    if (leftDriveValue > 0) {
                        leftDriveValue *= afLeft.readValue();
                    } else if (leftDriveValue < 0) {
                        leftDriveValue *= abLeft.readValue();
                    }
                    if (rightDriveValue > 0) {
                        rightDriveValue *= afRight.readValue();
                    } else if (rightDriveValue < 0) {
                        rightDriveValue *= abRight.readValue();
                    }
                } else {
                    if (leftDriveValue > 0) {
                        leftDriveValue *= gfLeft.readValue();
                    } else if (leftDriveValue < 0) {
                        leftDriveValue *= gbLeft.readValue();
                    }
                    if (rightDriveValue > 0) {
                        rightDriveValue *= gfRight.readValue();
                    } else if (rightDriveValue < 0) {
                        rightDriveValue *= gbRight.readValue();
                    }
                }

                //write motor values
                leftDrive1.writeValue(leftDriveValue);
                leftDrive2.writeValue(leftDriveValue);
                rightDrive1.writeValue(rightDriveValue);
                rightDrive2.writeValue(rightDriveValue);
            }
        });
    }

    public static void createShifting(EventSource begin, EventSource during, BooleanOutput shiftSolenoid, EventSource shiftHighButton, EventSource shiftLowButton) {
        final BooleanStatus shifted = new BooleanStatus(shiftSolenoid);

        //begin
        shifted.setTrueWhen(begin);

        //high
        shifted.setTrueWhen(shiftHighButton);

        //low
        shifted.setFalseWhen(shiftLowButton);
    }
}
