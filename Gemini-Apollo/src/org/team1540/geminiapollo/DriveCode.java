package org.team1540.geminiapollo;

import ccre.chan.BooleanOutput;
import ccre.chan.BooleanStatus;
import ccre.chan.FloatFilter;
import ccre.chan.FloatInputPoll;
import ccre.chan.FloatOutput;
import ccre.ctrl.Mixing;
import ccre.event.EventConsumer;
import ccre.event.EventSource;
import ccre.log.Logger;

public class DriveCode {

    /*TO DO:
     -add motor adjustors...
     -during for createShifting is currently unnecessary, might remove
     */
    public static void createDrive(EventSource begin, EventSource during, final FloatOutput leftDrive1, final FloatOutput leftDrive2, final FloatOutput rightDrive1, final FloatOutput rightDrive2, FloatInputPoll leftDriveAxis, FloatInputPoll rightDriveAxis, FloatInputPoll forwardDriveAxis, final boolean competitionRobot) {
        Logger.warning("Drivecode is swervy. In a bad way.");
        //dead zone
        FloatFilter deadZone = Mixing.deadzone(.05f);
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
                    /*leftDriveValue = leftDriveValue;
                     rightDriveValue = rightDriveValue;*/
                } else {
                    leftDriveValue = leftDriveValue * (619f / 697);
                    /*rightDriveValue = rightDriveValue;*/
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
        Logger.warning("DriveCode TOFINISH");
        final BooleanStatus shifted = new BooleanStatus(shiftSolenoid);

        //begin
        shifted.setTrueWhen(begin);

        //high
        shifted.setTrueWhen(shiftHighButton);

        //low
        shifted.setFalseWhen(shiftLowButton);
    }
}
