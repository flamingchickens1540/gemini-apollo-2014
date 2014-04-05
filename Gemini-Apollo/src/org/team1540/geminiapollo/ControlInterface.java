package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.ExpirationTimer;
import ccre.ctrl.IDispatchJoystick;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.TuningContext;
import ccre.phidget.PhidgetReader;

public class ControlInterface {

    private final IDispatchJoystick joystick1, joystick2;
    private static final FloatFilter driveDeadzone = Mixing.deadzone(.1f);
    private final Event forceArmLower = new Event();

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

    public BooleanInput getArmShouldBeDown(EventSource disabled) {
        BooleanStatus armIsDown = new BooleanStatus();
        armIsDown.setFalseWhen(joystick2.getButtonSource(5));
        armIsDown.setTrueWhen(joystick2.getButtonSource(6));
        armIsDown.setTrueWhen(forceArmLower);
        armIsDown.setFalseWhen(disabled);
        armIsDown.setFalseWhen(Mixing.whenBooleanBecomes(PhidgetReader.getDigitalInput(7), true));
        armIsDown.setTrueWhen(Mixing.whenBooleanBecomes(PhidgetReader.getDigitalInput(0), true));
        return armIsDown;
    }

    public BooleanInputPoll rollerIn() {
        return Mixing.orBooleans(PhidgetReader.getDigitalInput(3), Mixing.floatIsAtLeast(joystick2.getAxisChannel(2), 0.2f));
    }

    public BooleanInputPoll rollerOut() {
        return Mixing.orBooleans(PhidgetReader.getDigitalInput(4), Mixing.floatIsAtMost(joystick2.getAxisChannel(2), -0.2f));
    }

    public FloatInputPoll collectorSpeed() {
        /*final TuningContext tuner = new TuningContext(CluckGlobals.getNode(), "PowerSliderTuner");
        final FloatInput min = tuner.getFloat("Slider Min", 0f);
        final FloatInput max = tuner.getFloat("Slider Max", 1f);
        final FloatInput ai = PhidgetReader.getAnalogInput(4);
        return new FloatInputPoll() {
            public float readValue() {
                return normalize(min.readValue(), max.readValue(), ai.readValue());
            }
        };*/
        return Mixing.always(1);
    }

    public void showArm(BooleanInput arm) {
        arm.addTarget(Mixing.invert(PhidgetReader.digitalOutputs[0]));
        arm.addTarget(PhidgetReader.digitalOutputs[1]);
    }

    public void showFiring(EventSource when, BooleanInput canFire) {
        canFire.addTarget(PhidgetReader.digitalOutputs[2]);
    }

    public void displayPressureAndWinch(final FloatInputPoll level, EventSource update, final BooleanInputPoll cprSwitch, final FloatInputPoll currentSensor) {
        update.addListener(new EventConsumer() {
            int prevValue = -1000;
            int prevWinchValue = -1000;
            boolean prevValueCpr = cprSwitch.readValue();
            int ctr = 0;

            public void eventFired() {
                int c = (int) level.readValue();
                int winch = (int) (currentSensor.readValue());
                boolean cpr = cprSwitch.readValue();
                if (c == prevValue && (prevValueCpr == cpr) && prevWinchValue == winch && (ctr++ % 100 != 0)) {
                    return;
                }
                prevValue = c;
                prevValueCpr = cpr;
                prevWinchValue = winch;
                String mstr = c <= -10 ? "????" : Integer.toString(c) + "%";
                while (mstr.length() < 4) {
                    mstr = " " + mstr;
                }
                PhidgetReader.phidgetLCD[1].println("AIR " + (cpr ? "<" : " ") + mstr + (cpr ? ">" : " ") + " WNCH " + Float.toString(winch));
            }
        });
    }

    public static float normalize(float zero, float one, float value) {
        float range = one - zero;
        return ((value - zero) / range);
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

    public EventConsumer forceArmLower() {
        return forceArmLower;
    }

    public BooleanInputPoll shouldBeCollectingBecauseLoader() {
        ExpirationTimer exp = new ExpirationTimer();
        exp.startWhen(Mixing.whenBooleanBecomes(PhidgetReader.digitalInputs[0], true));
        BooleanStatus runCollector = new BooleanStatus();
        exp.scheduleBooleanPeriod(10, 510, runCollector, true);
        exp.schedule(520, exp.getStopEvent());
        return runCollector;
    }
}
