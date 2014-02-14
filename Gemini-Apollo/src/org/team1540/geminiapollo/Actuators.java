package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.chan.BooleanOutput;
import ccre.chan.FloatOutput;
import ccre.ctrl.Mixing;
import ccre.event.EventConsumer;
import ccre.event.EventSource;

public class Actuators {

    public static void createCollector(EventSource begin, EventSource during, FloatOutput collectorMotor, BooleanOutput armFloatSolenoid,final BooleanInputPoll rollersIn,final BooleanInputPoll rollersOut) {
        Mixing.pumpWhen(Mixing.filterEvent(rollersOut,false,during), rollersIn, armFloatSolenoid);
        Mixing.pumpWhen(Mixing.filterEvent(rollersIn,false,during), rollersOut, armFloatSolenoid);
    }

    public static void createArm(EventSource begin, EventSource during, BooleanOutput armSolenoid, BooleanInputPoll armUpDown, BooleanInputPoll canArmMove) {
        Mixing.pumpWhen(Mixing.filterEvent(canArmMove, true, during), armUpDown, armSolenoid);
    }
}
