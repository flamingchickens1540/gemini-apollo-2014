package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.chan.BooleanOutput;
import ccre.chan.FloatOutput;
import ccre.ctrl.Mixing;
import ccre.event.EventSource;

public class Actuators {

    public static void createCollector(EventSource begin, EventSource during, FloatOutput collectorMotor, BooleanOutput armFloatSolenoid, BooleanInputPoll rollersOnOff) {
        Mixing.pumpWhen(during, Mixing.select(rollersOnOff, 0f, 1f), collectorMotor);
        Mixing.pumpWhen(during, rollersOnOff, armFloatSolenoid);
    }

    public static void createArm(EventSource begin, EventSource during, BooleanOutput armSolenoid, BooleanInputPoll armUpDown,BooleanInputPoll canArmGoUp) {
        Mixing.pumpWhen(during, armUpDown, armSolenoid);
    }
}
