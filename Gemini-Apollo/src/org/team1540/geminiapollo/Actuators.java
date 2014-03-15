package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.ctrl.Mixing;
import ccre.event.*;

public class Actuators {

    private final EventSource during;

    public Actuators(EventSource updateDuring) {
        this.during = updateDuring;
    }

    public void createCollector(FloatOutput collectorMotor, FloatInputPoll speed, BooleanOutput armFloatSolenoid,
            final BooleanInputPoll rollersIn, final BooleanInputPoll rollersOut, BooleanInputPoll disableCollector) {
        during.addListener(Mixing.filterEvent(disableCollector, true, Mixing.filterEvent(rollersIn, true, new EventConsumer() {
            public void eventFired() {
                ErrorMessages.displayError(2, "Collect: not winch!", 200);
            }
        })));
        Mixing.pumpWhen(during, Mixing.quadSelect(rollersIn, rollersOut,
                Mixing.always(0f), Mixing.negate(speed),
                Mixing.select(disableCollector, speed, Mixing.always(0)), speed),
                collectorMotor);
        Mixing.pumpWhen(during, Mixing.orBooleans(rollersIn, rollersOut), armFloatSolenoid);
    }

    public void createArm(BooleanOutput armSolenoid, BooleanInputPoll armUpDown, BooleanInputPoll armDisabled) {
        Mixing.pumpWhen(Mixing.filterEvent(armDisabled, false, during), armUpDown, armSolenoid);
    }
}
