package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.TuningContext;

public class DriveCode {

    /*TO DO:
     -left motor adjustors flipped... figure out why
     */
    public static void createDrive(EventSource begin, EventSource during, final FloatOutput leftDrive1, final FloatOutput leftDrive2, final FloatOutput rightDrive1, final FloatOutput rightDrive2, FloatInputPoll leftDriveAxis, FloatInputPoll rightDriveAxis, FloatInputPoll forwardDriveAxis, final boolean competitionRobot, final BooleanStatus shifted) {
        //High Tuning
        TuningContext wheelTuner = new TuningContext(CluckGlobals.node, "TuningValues");
        wheelTuner.publishSavingEvent("Drive Tuning");
        final FloatStatus hfLeft = wheelTuner.getFloat("High Left Forwards", 1f);
        final FloatStatus hbLeft = wheelTuner.getFloat("High Left Backwards", 1f);
        final FloatStatus hfRight = wheelTuner.getFloat("High Right Forwards", 1f);
        final FloatStatus hbRight = wheelTuner.getFloat("High Right Backwards", 1f);

        //Low Tuning
        final FloatStatus lfLeft = wheelTuner.getFloat("Low Left Forwards", 1f);
        final FloatStatus lbLeft = wheelTuner.getFloat("Low Left Backwards", 1f);
        final FloatStatus lfRight = wheelTuner.getFloat("Low Right Forwards", 1f);
        final FloatStatus lbRight = wheelTuner.getFloat("Low Right Backwards", 1f);

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
                if (shifted.readValue()) {
                    if (leftDriveValue > 0) {
                        leftDriveValue *= hfLeft.readValue();
                    } else if (leftDriveValue < 0) {
                        leftDriveValue *= hbLeft.readValue();
                    }
                    if (rightDriveValue > 0) {
                        rightDriveValue *= hfRight.readValue();
                    } else if (rightDriveValue < 0) {
                        rightDriveValue *= hbRight.readValue();
                    }
                } else {
                    if (leftDriveValue > 0) {
                        leftDriveValue *= lfLeft.readValue();
                    } else if (leftDriveValue < 0) {
                        leftDriveValue *= lbLeft.readValue();
                    }
                    if (rightDriveValue > 0) {
                        rightDriveValue *= lfRight.readValue();
                    } else if (rightDriveValue < 0) {
                        rightDriveValue *= lbRight.readValue();
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

    public static BooleanStatus createShifting(EventSource begin, EventSource during, BooleanOutput shiftSolenoid, EventSource shiftHighButton, EventSource shiftLowButton) {
        final BooleanStatus shifted = new BooleanStatus(shiftSolenoid);

        //begin
        shifted.setFalseWhen(begin);

        //high
        shifted.setTrueWhen(shiftHighButton);

        //low
        shifted.setFalseWhen(shiftLowButton);

        return shifted;
    }
}
