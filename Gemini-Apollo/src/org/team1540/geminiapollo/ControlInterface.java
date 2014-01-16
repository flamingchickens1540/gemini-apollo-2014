package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.chan.FloatInputPoll;
import ccre.event.EventSource;
import ccre.log.Logger;

public class ControlInterface {

    public static BooleanInputPoll getArmUpDown() {
        Logger.warning("ControlInterface TODO");
        return null;
    }

    public static BooleanInputPoll getRollersOnOff() {
        Logger.warning("ControlInterface TODO");
        return null;
    }

    public static EventSource getRearmCatapult() {
        Logger.warning("ControlInterface TODO");
        return null;
    }

    public static EventSource getFireButton() {
        Logger.warning("ControlInterface TODO");
        return null;
    }
    
    // Added to control how much shooting power will be provided.
    // The returned FloatInputPoll should return in the range 0.0 - 1.0.
    public static FloatInputPoll getPullbackSlider() {
        Logger.warning("ControlInterface TODO");
        return null;
    }
}
