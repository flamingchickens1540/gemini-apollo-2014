package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.ctrl.Mixing;
import ccre.event.EventSource;

public class Actuators {

    public static void createCollector(EventSource begin, EventSource during, FloatOutput collectorMotor, BooleanOutput armFloatSolenoid, final BooleanInputPoll rollersIn, final BooleanInputPoll rollersOut) {
        Mixing.pumpWhen(during, Mixing.select(rollersIn, Mixing.select(rollersOut, 0f, -1f), Mixing.always(1f)), collectorMotor);
        Mixing.pumpWhen(during, Mixing.orBooleans(rollersIn, rollersOut), armFloatSolenoid);
    }

    public static void createArm(EventSource begin, EventSource during, BooleanOutput armSolenoid, BooleanInputPoll armUpDown, BooleanInputPoll canArmMove) {
        Mixing.pumpWhen(Mixing.filterEvent(canArmMove, true, during), armUpDown, armSolenoid);
    }
}
