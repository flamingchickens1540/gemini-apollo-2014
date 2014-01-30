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
    public static FloatInputPoll displayPressure(final FloatInputPoll f,EventSource update){
        final FloatInputPoll pressure = Mixing.normalizeFloat(f, 100, 587);
        update.addListener(new EventConsumer(){
            public void eventFired() {
                PhidgetReader.phidgetLCD[1].println("Pressure: "+pressure.readValue ()+"%");
            }
        });
        return pressure;
    }
}
