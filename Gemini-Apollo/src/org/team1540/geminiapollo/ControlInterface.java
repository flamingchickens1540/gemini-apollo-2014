package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.chan.FloatInputPoll;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.Mixing;
import ccre.event.EventConsumer;
import ccre.event.EventSource;
import ccre.holders.TuningContext;
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

    public static void displayPressure(final FloatInputPoll f, EventSource update) {
        final TuningContext tuner = new TuningContext(CluckGlobals.node, "PressureTuner");
        tuner.publishSavingEvent("Pressure");
        //0.5
        final FloatInputPoll zeroP = tuner.getFloat("LowPressure", -3f);
        //2.745
        final FloatInputPoll oneP = tuner.getFloat("HighPressure", 3f);
        update.addListener(new EventConsumer() {
            int prevValue = -1000;
            int ctr = 0;

            public void eventFired() {
                float zero = zeroP.readValue();
                float one = oneP.readValue();
                float range = one - zero;
                int c = (int) (1000 * (f.readValue() - zero) / range);
                if (c == prevValue && (ctr++ % 100 != 0)) {
                    return;
                }
                prevValue = c;
                PhidgetReader.phidgetLCD[1].println("Pressure: " + c / 10f + "%");
            }
        });
    }
}
