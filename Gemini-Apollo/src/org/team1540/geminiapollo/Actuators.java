package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.chan.BooleanOutput;
import ccre.chan.FloatOutput;
import ccre.ctrl.Mixing;
import ccre.event.EventSource;

public class Actuators {

    public static void createCollector(EventSource begin, EventSource during, FloatOutput collectorMotor, BooleanInputPoll rollersOnOff) {
        Mixing.pumpWhen(during, Mixing.select(rollersOnOff, 0f, 1f), collectorMotor);
    }

    public static void createArm(EventSource begin, EventSource during, BooleanOutput armSolenoid, BooleanInputPoll armUpDown) {
        Mixing.pumpWhen(during, armUpDown, armSolenoid);
    }
}
