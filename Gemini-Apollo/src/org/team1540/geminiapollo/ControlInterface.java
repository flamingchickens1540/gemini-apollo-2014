package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.TuningContext;
import ccre.phidget.PhidgetReader;

public class ControlInterface {

    public static BooleanInput getRearmCatapult() {
        return PhidgetReader.getDigitalInput(0);
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
        return PhidgetReader.getAnalogInput(0);
    }

    public static BooleanInput detensioning() {
        return PhidgetReader.getDigitalInput(7);
    }

    public static void displayDistance(final FloatInputPoll distance, EventSource update) {
        update.addListener(new EventConsumer() {
            float last = -1;
            int ctr = 0;

            public void eventFired() {
                float value = distance.readValue();
                if (value == last && (ctr++ % 100 != 0)) {
                    return;
                }
                PhidgetReader.phidgetLCD[0].println(value);
                last = value;
            }
        });
    }

    public static void displayPressure(final FloatInputPoll f, EventSource update, final BooleanInputPoll cprSwitch) {
        final TuningContext tuner = new TuningContext(CluckGlobals.node, "PressureTuner");
        tuner.publishSavingEvent("Pressure");
        final FloatInputPoll zeroP = tuner.getFloat("LowPressure", 0.494f); // 0.5
        final FloatInputPoll oneP = tuner.getFloat("HighPressure", 2.746f); // 2.745
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

    public static void showRearming(EventSource when, BooleanInputPoll isRearming) {
        Mixing.pumpWhen(when, Mixing.invert(isRearming), new BooleanStatus(PhidgetReader.digitalOutputs[1]));
    }

    public static void showFiring(EventSource when, BooleanInput canFire) {
        Mixing.pumpWhen(when, Mixing.invert(canFire), new BooleanStatus(PhidgetReader.digitalOutputs[0]));
    }

    private static int normalize(float zero, float one, float value) {
        float range = one - zero;
        return (int) (100 * (value - zero) / range);
    }
}
