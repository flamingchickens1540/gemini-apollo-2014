package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.chan.FloatInputPoll;
import ccre.ctrl.Mixing;
import ccre.event.EventConsumer;
import ccre.event.EventSource;
import ccre.phidget.PhidgetReader;

public class ControlInterface {

    public static BooleanInputPoll getArmUpDown() {
        return PhidgetReader.getDigitalInput(2);
    }

    public static BooleanInputPoll getRollersOnOff() {
        return PhidgetReader.getDigitalInput(3);
    }

    public static EventSource getRearmCatapult() {
        return Mixing.whenBooleanBecomes(PhidgetReader.digitalInputs[0], true);
    }

    public static EventSource getFireButton() {
        return Mixing.whenBooleanBecomes(PhidgetReader.digitalInputs[1], true);
    }

    public static FloatInputPoll displayPressure(final FloatInputPoll f, EventSource update) {
        final FloatInputPoll pressure = Mixing.normalizeFloat(f, -3f, 3f);
        update.addListener(new EventConsumer() {
            int prevValue = -1000;
            int ctr = 0;
            public void eventFired() {
                int c = (int) (1000 * pressure.readValue());
                if (c == prevValue && (ctr++ % 100 != 0)) {
                    return;
                }
                prevValue = c;
                PhidgetReader.phidgetLCD[1].println("Pressure: " + prevValue / 10f + "%");
            }
        });
        return pressure;
    }
}
