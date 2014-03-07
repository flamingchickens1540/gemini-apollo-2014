package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.Mixing;
import ccre.event.*;

public class Actuators {

    public static void createCollector(EventSource during, FloatOutput collectorMotor, BooleanOutput armFloatSolenoid,
            final BooleanInputPoll rollersIn, final BooleanInputPoll rollersOut, BooleanInputPoll canCollectorRun) {
        Mixing.pumpWhen(during, Mixing.select(canCollectorRun, Mixing.always(0f), Mixing.quadSelect(rollersIn, rollersOut, 0f, -1f, 1f, 1f)), collectorMotor);
        Mixing.pumpWhen(during, Mixing.orBooleans(rollersIn, rollersOut), armFloatSolenoid);
    }

    public static void createArm(EventSource during, BooleanOutput armSolenoid, BooleanInputPoll armUpDown, BooleanInputPoll canArmMove) {
        Mixing.pumpWhen(Mixing.filterEvent(canArmMove, true, during), armUpDown, armSolenoid);
    }

    public static BooleanInputPoll calcCompressorControl(EventSource counter, final BooleanInputPoll pressureSwitch) {
        final FloatStatus compressorOverride = new FloatStatus();
        CluckGlobals.node.publish("Compressor Override", compressorOverride);
        CluckGlobals.node.publish("Compressor Sensor", Mixing.createDispatch(pressureSwitch, counter));
        counter.addListener(new EventConsumer() {
            public void eventFired() {
                float value = compressorOverride.readValue();
                if (value > 0) {
                    if (value > 10) {
                        value = 10;
                    }
                    value -= 0.01;
                    if (value < 0) {
                        value = 0;
                    }
                }
                compressorOverride.writeValue(value);
            }
        });
        return new BooleanInputPoll() {
            public boolean readValue() {
                float value = compressorOverride.readValue();
                if (value < 0) {
                    return true;
                } else if (value > 0) {
                    return false;
                } else {
                    return pressureSwitch.readValue();
                }
            }
        };
    }
}
