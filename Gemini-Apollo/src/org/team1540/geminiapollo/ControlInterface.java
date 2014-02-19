package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.TuningContext;
import ccre.phidget.PhidgetReader;

public class ControlInterface {

    public static EventSource getRearmCatapult() {
        return Mixing.whenBooleanBecomes(PhidgetReader.digitalInputs[0], true);
    }

    public static EventSource getFireButton() {
        return Mixing.whenBooleanBecomes(PhidgetReader.digitalInputs[1], true);
    }

    public static BooleanInputPoll getArmUpDown() {
        return PhidgetReader.getDigitalInput(2);
    }

    public static BooleanInputPoll rollerIn() {
        return PhidgetReader.getDigitalInput(3);
    }

    public static BooleanInputPoll rollerOut() {
        return PhidgetReader.getDigitalInput(4);
    }

    public static FloatInputPoll powerSlider() {
        //values range from roughly 0 to 1
        return Mixing.normalizeFloat(PhidgetReader.getAnalogInput(4), -.974f, .934f);
    }

    public static BooleanInputPoll detensioning() {
        return PhidgetReader.getDigitalInput(7);
    }

    public static void displayPressure(final FloatInputPoll f, final BooleanInputPoll cprSwitch, EventSource update) {
        final TuningContext tuner = new TuningContext(CluckGlobals.node, "PressureTuner");
        tuner.publishSavingEvent("Pressure");
        //0.5
        final FloatInputPoll zeroP = tuner.getFloat("LowPressure", 0.494f);
        //2.745
        final FloatInputPoll oneP = tuner.getFloat("HighPressure", 2.746f);
        update.addListener(new EventConsumer() {
            int prevValue = -1000;
            boolean prevValueCpr = cprSwitch.readValue();
            int ctr = 0;

            public void eventFired() {
                int c = normalize(zeroP.readValue(), oneP.readValue(), f.readValue());
                boolean cpr = cprSwitch.readValue();
                if (c == prevValue && (prevValueCpr == cpr) && (ctr++ % 100 != 0)) {
                    return;
                }
                prevValue = c;
                prevValueCpr = cpr;
                String mstr = c <= -10 ? "????" : Integer.toString(c) + "%";
                while (mstr.length() < 4) {
                    mstr = " " + mstr;
                }
                PhidgetReader.phidgetLCD[1].println("Air: " + (cpr ? "<" : " ") + mstr + (cpr ? "> " : "  ") + (RobotMain.IS_COMPETITION_ROBOT ? "(APOLLO)" : "(GEMINI)"));
            }
        });
    }

    private static int normalize(float zero, float one, float value) {
        float range = one - zero;
        return (int) (100 * (value - zero) / range);
    }
}
