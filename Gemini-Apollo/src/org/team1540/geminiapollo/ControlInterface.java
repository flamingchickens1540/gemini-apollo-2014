package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.chan.FloatInputPoll;
import ccre.ctrl.Mixing;
import ccre.event.EventSource;
import ccre.log.Logger;
import ccre.phidget.PhidgetReader;

public class ControlInterface {

    public static BooleanInputPoll getArmUpDown() {
        return PhidgetReader.getDigitalInput(0);
    }

    public static BooleanInputPoll getRollersOnOff() {
        return PhidgetReader.getDigitalInput(0);
    }

    public static EventSource getRearmCatapult() {
        return Mixing.whenBooleanBecomes(PhidgetReader.digitalInputs[2], true);
    }

    public static EventSource getFireButton() {
        return Mixing.whenBooleanBecomes(PhidgetReader.digitalInputs[3], true);
    }
}
