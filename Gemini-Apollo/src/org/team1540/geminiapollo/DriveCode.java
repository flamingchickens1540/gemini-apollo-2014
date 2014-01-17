package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
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
     -make sure shifting boolean is correct
     -add motor adjustors...
     */
    public static void createDrive(EventSource begin, EventSource during, final FloatOutput leftDrive1, final FloatOutput leftDrive2, final FloatOutput rightDrive1, final FloatOutput rightDrive2, FloatInputPoll leftDriveAxis, FloatInputPoll rightDriveAxis, FloatInputPoll forwardDriveAxis) {
        Logger.warning("DriveCode TODO");
        //dead zone
        FloatFilter deadZone = Mixing.deadzone(.05f);
        final FloatInputPoll leftDriveAxisW = deadZone.wrap(leftDriveAxis);
        final FloatInputPoll rightDriveAxisW = deadZone.wrap(rightDriveAxis);
        final FloatInputPoll forwardDriveAxisW = deadZone.wrap(forwardDriveAxis);

        begin.addListener(new EventConsumer() {
            public void eventFired() {
                leftDrive1.writeValue(0);
                leftDrive2.writeValue(0);
                rightDrive1.writeValue(0);
                rightDrive2.writeValue(0);
            }
        });

        during.addListener(new EventConsumer() {
            public void eventFired() {
                //motor values
                float leftDrive1Value = (leftDriveAxisW.readValue() + forwardDriveAxisW.readValue());
                float leftDrive2Value = (leftDriveAxisW.readValue() + forwardDriveAxisW.readValue());
                float rightDrive1Value = (rightDriveAxisW.readValue() + forwardDriveAxisW.readValue());
                float rightDrive2Value = (rightDriveAxisW.readValue() + forwardDriveAxisW.readValue());

                //adjust motor values
                //write motor values
                leftDrive1.writeValue(leftDrive1Value);
                leftDrive2.writeValue(leftDrive2Value);
                rightDrive1.writeValue(rightDrive1Value);
                rightDrive2.writeValue(rightDrive2Value);
            }
        });
    }

    public static void createShifting(EventSource begin, EventSource during, BooleanOutput shiftSolenoid, EventSource shiftHighButton, EventSource shiftLowButton) {
        Logger.warning("DriveCode TODO");
        final BooleanStatus shifted = new BooleanStatus(shiftSolenoid);
        //begin
        shifted.setTrueWhen(begin);
        //high
        shifted.setTrueWhen(shiftHighButton);
        //low
        shifted.setFalseWhen(shiftLowButton);
    }
}
