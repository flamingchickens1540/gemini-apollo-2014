package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.IDispatchJoystick;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.TuningContext;
import ccre.phidget.PhidgetReader;

public class ControlInterface {

    public static IDispatchJoystick joystick;
    public static BooleanStatus armStatus = new BooleanStatus(false);

    public static BooleanInput getRearmCatapult(EventSource update) {
        BooleanInputPoll a = PhidgetReader.getDigitalInput(2);
        BooleanInputPoll b = joystick.getButtonChannel(1);
        return Mixing.createDispatch(Mixing.xorBooleans(a, b), update);
    }

    public static EventSource getFireButton() {
        return Mixing.combine(Mixing.whenBooleanBecomes(PhidgetReader.digitalInputs[1], true), joystick.getButtonSource(2));
    }

    public static BooleanInputPoll getArmUpDown() {
        armStatus.setFalseWhen(joystick.getButtonSource(5));
        armStatus.setTrueWhen(joystick.getButtonSource(6));
        armStatus.setFalseWhen(Mixing.whenBooleanBecomes(PhidgetReader.getDigitalInput(0), true));
        armStatus.setTrueWhen(Mixing.whenBooleanBecomes(PhidgetReader.getDigitalInput(7), true));
        return armStatus;
    }

    public static BooleanInputPoll rollerIn() {
        return Mixing.orBooleans(PhidgetReader.getDigitalInput(3), Mixing.floatIsAtLeast(joystick.getAxisChannel(2), 0.2f));
    }

    public static BooleanInputPoll rollerOut() {
        return Mixing.orBooleans(PhidgetReader.getDigitalInput(4), Mixing.floatIsAtMost(joystick.getAxisChannel(2), -0.2f));
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

    public static FloatInputPoll collectorSpeed() {
        final TuningContext tuner = new TuningContext(CluckGlobals.node, "PowerSliderTuner");
        CluckGlobals.node.publish("Slider Power", PhidgetReader.getAnalogInput(4));
        return new FloatInputPoll() {
            public float readValue() {
                return ControlInterface.normalize(tuner.getFloat("Min", 0f).readValue(), tuner.getFloat("Max", 1f).readValue(), PhidgetReader.getAnalogInput(4).readValue());
            }
        };
    }

    public static void showSwitch(EventSource when) {
        Mixing.pumpEvent(armStatus, PhidgetReader.digitalOutputs[2]);
    }

    public static void showRearming(EventSource when, BooleanInputPoll isRearming) {
        //Mixing.pumpWhen(when, Mixing.invert(isRearming), new BooleanStatus(PhidgetReader.digitalOutputs[1]));
    }

    public static void showFiring(EventSource when, BooleanInput canFire) {
        Mixing.pumpWhen(when, Mixing.invert(canFire), new BooleanStatus(PhidgetReader.digitalOutputs[0]));
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
                int c = (int) normalize(zeroP.readValue(), oneP.readValue(), f.readValue());
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

    private static float normalize(float zero, float one, float value) {
        float range = one - zero;
        return (int) (100 * (value - zero) / range);
    }

    private static int errno = 0;

    public static void displayError(String message) {
        errno = (errno + 1) % 10;
        PhidgetReader.phidgetLCD[0].print(errno + ' ' + message + '\n');
    }
}
