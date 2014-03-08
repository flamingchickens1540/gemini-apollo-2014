package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.TuningContext;

public class DriveCode {

    public static void createDrive(EventSource begin, EventSource during, FloatOutput leftDrive1, FloatOutput leftDrive2, FloatOutput rightDrive1, FloatOutput rightDrive2, FloatInputPoll leftDriveAxis, FloatInputPoll rightDriveAxis, FloatInputPoll forwardDriveAxis, final boolean competitionRobot, final BooleanStatus notShifted) {
        final FloatOutput leftDrive = Mixing.combine(leftDrive1, leftDrive2);
        final FloatOutput rightDrive = Mixing.combine(rightDrive1, rightDrive2);
        //High Tuning
        TuningContext wheelTuner = new TuningContext(CluckGlobals.node, "DriveTuning");
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
        Mixing.setWhen(begin, leftDrive, 0);
        Mixing.setWhen(begin, rightDrive, 0);
        //during
        during.addListener(new EventConsumer() {
            public void eventFired() {
                //motor values
                float leftDriveValue = leftDriveAxisW.readValue() + forwardDriveAxisW.readValue();
                float rightDriveValue = rightDriveAxisW.readValue() + forwardDriveAxisW.readValue();
                //adjust motor values
                if (!notShifted.readValue()) {
                    leftDriveValue *= leftDriveValue > 0 ? hfLeft.readValue() : hbLeft.readValue();
                    rightDriveValue *= rightDriveValue > 0 ? hfRight.readValue() : hbRight.readValue();
                } else {
                    leftDriveValue *= leftDriveValue > 0 ? lfLeft.readValue() : lbLeft.readValue();
                    rightDriveValue *= rightDriveValue > 0 ? lfRight.readValue() : lbRight.readValue();
                }
                //write motor values
                leftDrive.writeValue(leftDriveValue);
                rightDrive.writeValue(rightDriveValue);
            }
        });
    }

    public static BooleanStatus createShifting(EventSource begin, EventSource beginAuto, EventSource during, BooleanOutput shiftSolenoid, EventSource shiftHighButton, EventSource shiftLowButton) {
        final BooleanStatus shifted = new BooleanStatus(shiftSolenoid);
        shifted.setTrueWhen(begin); // begin
        shifted.setTrueWhen(beginAuto);
        shifted.setTrueWhen(shiftHighButton); // high - THESE ARE WRONG
        shifted.setFalseWhen(shiftLowButton); // low
        return shifted;
    }
}
