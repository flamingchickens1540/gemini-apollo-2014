package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.chan.BooleanOutput;
import ccre.chan.FloatFilter;
import ccre.chan.FloatInputPoll;
import ccre.chan.FloatOutput;
import ccre.ctrl.Mixing;
import ccre.log.Logger;

public class DriveCode {

    public static void createDrive(FloatOutput leftDrive1, FloatOutput leftDrive2, FloatOutput rightDrive1, FloatOutput rightDrive2, FloatInputPoll leftDriveAxis, FloatInputPoll rightDriveAxis, FloatInputPoll forwardDriveAxis) {
        Logger.warning("DriveCode TODO");
        //dead zone
        FloatFilter deadZone = Mixing.deadzone(.05f);
        FloatInputPoll leftDriveAxisW = deadZone.wrap(leftDriveAxis);
        FloatInputPoll rightDriveAxisW = deadZone.wrap(rightDriveAxis);
        FloatInputPoll forwardDriveAxisW = deadZone.wrap(forwardDriveAxis);
        
        //motor values
        float leftDriveValue1;
        float leftDriveValue2;
        float rightDriveValue1;
        float rightDriveValue2;
        if (forwardDriveAxisW.readValue() != 0) {
            leftDriveValue1 = forwardDriveAxisW.readValue();
            leftDriveValue2 = forwardDriveAxisW.readValue();
            rightDriveValue1 = forwardDriveAxisW.readValue();
            rightDriveValue2 = forwardDriveAxisW.readValue();
        } else {
            leftDriveValue1 = leftDriveAxisW.readValue();
            leftDriveValue2 = leftDriveAxisW.readValue();
            rightDriveValue1 = rightDriveAxisW.readValue();
            rightDriveValue2 = rightDriveAxisW.readValue();
        }
        
        //adjust motor values
        
        //write motor values
        leftDrive1.writeValue(leftDriveValue1);
        leftDrive2.writeValue(leftDriveValue2);
        rightDrive1.writeValue(rightDriveValue1);
        rightDrive2.writeValue(rightDriveValue2);
    }

    public static void createShifting(BooleanOutput shiftSolenoid, BooleanInputPoll shiftHighButton, BooleanInputPoll shiftLowButton) {
        Logger.warning("DriveCode TODO");
    }
}
