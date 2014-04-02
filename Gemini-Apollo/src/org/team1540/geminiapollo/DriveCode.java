package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.event.*;
import ccre.holders.TuningContext;

public class DriveCode {

    public static void createDrive(EventSource begin, EventSource during, final FloatOutput leftDrive, final FloatOutput rightDrive, final FloatInputPoll leftDriveAxis, final FloatInputPoll rightDriveAxis, final FloatInputPoll forwardDriveAxis, final BooleanStatus notShifted) {
        TuningContext wheelTuner = new TuningContext(CluckGlobals.getNode(), "DriveTuning");
        wheelTuner.publishSavingEvent("Drive Tuning");
        final FloatStatus hfLeft = wheelTuner.getFloat("High Left Fwd", 1f), hfRight = wheelTuner.getFloat("High Right Fwd", 1f);
        final FloatStatus hbLeft = wheelTuner.getFloat("High Left Bck", 1f), hbRight = wheelTuner.getFloat("High Right Bck", 1f);
        final FloatStatus lfLeft = wheelTuner.getFloat("Low Left Fwd", 1f), lfRight = wheelTuner.getFloat("Low Right Fwd", 1f);
        final FloatStatus lbLeft = wheelTuner.getFloat("Low Left Bck", 1f), lbRight = wheelTuner.getFloat("Low Right Bck", 1f);

        during.addListener(new EventConsumer() {
            public void eventFired() {
                float leftDriveValue = leftDriveAxis.readValue() + forwardDriveAxis.readValue();
                float rightDriveValue = rightDriveAxis.readValue() + forwardDriveAxis.readValue();
                if (!notShifted.readValue()) {
                    leftDriveValue *= leftDriveValue > 0 ? hfLeft.readValue() : hbLeft.readValue();
                    rightDriveValue *= rightDriveValue > 0 ? hfRight.readValue() : hbRight.readValue();
                } else {
                    leftDriveValue *= leftDriveValue > 0 ? lfLeft.readValue() : lbLeft.readValue();
                    rightDriveValue *= rightDriveValue > 0 ? lfRight.readValue() : lbRight.readValue();
                }
                leftDrive.writeValue(leftDriveValue);
                rightDrive.writeValue(rightDriveValue);
            }
        });
    }

    public static BooleanStatus createShifting(EventSource begin, EventSource beginAuto, EventSource during, BooleanOutput shiftSolenoid, EventSource shiftHighButton, EventSource shiftLowButton) {
        final BooleanStatus shifted = new BooleanStatus(shiftSolenoid);
        shifted.setTrueWhen(begin); // begin
        shifted.setFalseWhen(beginAuto);
        shifted.setTrueWhen(shiftHighButton); // high - APOLLO WILL BE CHANGED SO THAT THESE ARE CORRECT
        shifted.setFalseWhen(shiftLowButton); // low
        return shifted;
    }
}
