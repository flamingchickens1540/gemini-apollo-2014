package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.chan.FloatInputPoll;
import ccre.ctrl.Mixing;
import ccre.event.EventSource;
import ccre.log.Logger;
import ccre.phidget.PhidgetReader;

public class ControlInterface {

    public static BooleanInputPoll getArmUpDown() {
        Logger.warning("ControlInterface WIP");
        return (BooleanInputPoll) PhidgetReader.digitalInputs[0];
    }

    public static BooleanInputPoll getRollersOnOff() {
        Logger.warning("ControlInterface WIP");
        return (BooleanInputPoll) PhidgetReader.digitalInputs[1];
    }

    public static EventSource getRearmCatapult() {
        Logger.warning("ControlInterface WIPa");
        return Mixing.whenBooleanBecomes(PhidgetReader.digitalInputs[2], true);
    }

    public static EventSource getFireButton() {
        Logger.warning("ControlInterface WIP");
        return Mixing.whenBooleanBecomes(PhidgetReader.digitalInputs[3], true);
    }
    
    // Added to control how much shooting power will be provided.
    // The returned FloatInputPoll should return in the range 0.0 - 1.0.
    public static FloatInputPoll getPullbackSlider() {
        Logger.warning("ControlInterface TODO(Uncalibarated)");
           return Mixing.normalizeFloat((FloatInputPoll)PhidgetReader.analogInputs[0],0,1);
    }
}
