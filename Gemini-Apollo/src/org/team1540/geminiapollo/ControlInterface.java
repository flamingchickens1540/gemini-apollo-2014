package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.IDispatchJoystick;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.TuningContext;
import ccre.phidget.PhidgetReader;

public class ControlInterface {

    private final IDispatchJoystick joystick1, joystick2;
    private static FloatFilter driveDeadzone = Mixing.deadzone(.1f);

    public ControlInterface(IDispatchJoystick joystick1, IDispatchJoystick joystick2) {
        this.joystick1 = joystick1;
        this.joystick2 = joystick2;
    }

    public BooleanInput getRearmCatapult(EventSource update) {
        BooleanInputPoll a = PhidgetReader.getDigitalInput(2);
        BooleanInputPoll b = joystick2.getButtonChannel(1);
        return Mixing.createDispatch(Mixing.xorBooleans(a, b), update);
    }

    public EventSource getFireButton() {
        return Mixing.combine(Mixing.whenBooleanBecomes(PhidgetReader.digitalInputs[1], true), joystick2.getButtonSource(2));
    }

    public BooleanInput getArmShouldBeDown() {
        BooleanStatus armIsDown = new BooleanStatus();
        armIsDown.setFalseWhen(joystick2.getButtonSource(5));
        armIsDown.setTrueWhen(joystick2.getButtonSource(6));
        armIsDown.setFalseWhen(Mixing.whenBooleanBecomes(PhidgetReader.getDigitalInput(0), true));
        armIsDown.setTrueWhen(Mixing.whenBooleanBecomes(PhidgetReader.getDigitalInput(7), true));
        return armIsDown;
    }

    public BooleanInputPoll rollerIn() {
        return Mixing.orBooleans(PhidgetReader.getDigitalInput(3), Mixing.floatIsAtLeast(joystick2.getAxisChannel(2), 0.2f));
    }

    public BooleanInputPoll rollerOut() {
        return Mixing.orBooleans(PhidgetReader.getDigitalInput(4), Mixing.floatIsAtMost(joystick2.getAxisChannel(2), -0.2f));
    }

    public void displayDistance(final FloatInputPoll distance, EventSource update) {
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

    public FloatInputPoll collectorSpeed() {
        final TuningContext tuner = new TuningContext(CluckGlobals.node, "PowerSliderTuner");
        final FloatInput min = tuner.getFloat("Min", 0f);
        final FloatInput max = tuner.getFloat("Max", 1f);
        final FloatInput ai = PhidgetReader.getAnalogInput(4);
        return new FloatInputPoll() {
            public float readValue() {
                return normalize(min.readValue(), max.readValue(), ai.readValue());
            }
        };
    }

    public void showArm(BooleanInput arm) {
        arm.addTarget(PhidgetReader.digitalOutputs[2]);
    }

    public void showFiring(EventSource when, BooleanInput canFire) {
        Mixing.invert(canFire).addTarget(PhidgetReader.digitalOutputs[0]);
    }

    public void displayPressure(final FloatInputPoll f, EventSource update, final BooleanInputPoll cprSwitch) {
        final TuningContext tuner = new TuningContext(CluckGlobals.node, "PressureTuner");
        tuner.publishSavingEvent("Pressure");
        final FloatInputPoll zeroP = tuner.getFloat("LowPressure", 0.494f); // 0.5
        final FloatInputPoll oneP = tuner.getFloat("HighPressure", 2.746f); // 2.745
        update.addListener(new EventConsumer() {
            int prevValue = -1000;
            boolean prevValueCpr = cprSwitch.readValue();
            int ctr = 0;

            public void eventFired() {
                int c = (int) (100 * normalize(zeroP.readValue(), oneP.readValue(), f.readValue()));
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

    private float normalize(float zero, float one, float value) {
        float range = one - zero;
        return ((value - zero) / range);
    }

    private static int errno = 0;

    public static void displayError(String message) {
        errno = (errno + 1) % 10;
        PhidgetReader.phidgetLCD[0].print(errno + " " + message + '\n');
    }

    public FloatInputPoll getLeftDriveAxis() {
        return driveDeadzone.wrap(Mixing.negate(joystick1.getAxisChannel(2)));
    }

    public FloatInputPoll getRightDriveAxis() {
        return driveDeadzone.wrap(Mixing.negate(joystick1.getAxisChannel(5)));
    }

    public FloatInputPoll getForwardDriveAxis() {
        return driveDeadzone.wrap(Mixing.negate(joystick1.getAxisChannel(3)));
    }

    public EventSource getShiftHighButton() {
        return joystick1.getButtonSource(1);
    }

    public EventSource getShiftLowButton() {
        return joystick1.getButtonSource(3);
    }
}
