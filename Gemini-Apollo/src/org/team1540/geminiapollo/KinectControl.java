package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.IDispatchJoystick;
import ccre.ctrl.Mixing;
import ccre.event.EventSource;

public class KinectControl {

    static BooleanInputPoll main(IDispatchJoystick disp1, IDispatchJoystick disp2, EventSource globalPeriodic) {
        CluckGlobals.node.publish("stick-1", disp1.getAxisSource(2));
        CluckGlobals.node.publish("stick-2", disp2.getAxisSource(2));
        BooleanInputPoll pressed = Mixing.andBooleans(
                Mixing.floatIsAtMost(disp1.getAxisChannel(2), -0.1f),
                Mixing.floatIsAtMost(disp2.getAxisChannel(2), -0.1f));
        CluckGlobals.node.publish("stick-pressed", Mixing.createDispatch(pressed, globalPeriodic));
        return pressed;
    }

}
