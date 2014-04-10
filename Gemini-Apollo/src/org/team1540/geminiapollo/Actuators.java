package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.TuningContext;
import ccre.instinct.AutonomousModeOverException;
import ccre.instinct.InstinctModule;
import ccre.log.Logger;

public class Actuators {

    private final EventSource during;
    public final EventConsumer armUp, armDown, armAlign;
    private final TuningContext actuatorContext = new TuningContext(CluckGlobals.getNode(), "Actuators");
    private final FloatStatus movementUpDelay = actuatorContext.getFloat("arm-up-delay", 0.3f);
    private final FloatStatus movementDownDelay = actuatorContext.getFloat("arm-hover-delay", 0.6f);
    public static final int STATE_UP = 0, STATE_DOWN = 1, STATE_ALIGN = 2;

    public Actuators(BooleanInputPoll shouldBeRunning, final BooleanInputPoll isTeleop, EventSource updateDuring, final BooleanOutput isSafeToShoot, final BooleanOutput isArmLower,
            final BooleanOutput isArmRaise, final BooleanOutput armMain, final BooleanOutput armLock) {
        this.during = updateDuring;
        final BooleanStatus pressedUp = new BooleanStatus(), pressedDown = new BooleanStatus(), pressedAlign = new BooleanStatus();
        armUp = pressedUp.getSetTrueEvent();
        armDown = pressedDown.getSetTrueEvent();
        armAlign = pressedAlign.getSetTrueEvent();
        new InstinctModule(shouldBeRunning) {
            private void resetInputs() {
                pressedUp.writeValue(false);
                pressedDown.writeValue(false);
                pressedAlign.writeValue(false);
            }

            protected String getTypeName() {
                return "actuator control loop";
            }

            protected void autonomousMain() throws AutonomousModeOverException, InterruptedException {
                if (isTeleop.readValue()) {
                    isArmLower.writeValue(false);
                    isArmRaise.writeValue(false);
                    waitForTime(80);
                    isArmLower.writeValue(true);
                    isArmRaise.writeValue(false);
                    waitForTime(80);
                    isArmLower.writeValue(false);
                    isArmRaise.writeValue(true);
                    waitForTime(80);
                    isArmLower.writeValue(false);
                    isArmRaise.writeValue(false);
                    waitForTime(80);
                }
                int next = 0;
                while (true) {
                    resetInputs();
                    isSafeToShoot.writeValue(next != STATE_UP);
                    if (next == STATE_UP) { // Up
                        Logger.fine("Actuator state: UP");
                        armLock.writeValue(false);
                        armMain.writeValue(false);
                        isArmLower.writeValue(true);
                        isArmRaise.writeValue(false);
                        if (waitUntilOneOf(new BooleanInputPoll[]{pressedDown, pressedAlign}) == 0) {
                            next = STATE_DOWN;
                        } else {
                            isArmLower.writeValue(true);
                            isArmRaise.writeValue(true);
                            armMain.writeValue(true);
                            waitForTime((long) (movementDownDelay.readValue() * 1000L));
                            next = STATE_ALIGN;
                        }
                    } else if (next == 1) { // Down
                        Logger.fine("Actuator state: DOWN");
                        armLock.writeValue(false);
                        armMain.writeValue(true);
                        isArmLower.writeValue(false);
                        isArmRaise.writeValue(true);
                        if (waitUntilOneOf(new BooleanInputPoll[]{pressedUp, pressedAlign}) == 0) {
                            next = STATE_UP;
                        } else {
                            next = STATE_ALIGN;
                        }
                    } else if (next == 2) { // Align
                        Logger.fine("Actuator state: ALIGN");
                        armLock.writeValue(true);
                        armMain.writeValue(false);
                        isArmLower.writeValue(false);
                        isArmRaise.writeValue(false);
                        if (waitUntilOneOf(new BooleanInputPoll[]{pressedDown, pressedUp}) == 0) {
                            next = STATE_DOWN;
                        } else {
                            isSafeToShoot.writeValue(false);
                            isArmLower.writeValue(true);
                            isArmRaise.writeValue(true);
                            armMain.writeValue(true);
                            waitForTime((long) (movementUpDelay.readValue() * 1000L));
                            next = STATE_UP;
                        }
                    } else {
                        next = STATE_UP;
                        Logger.warning("Bad Actuator state! Resetting to up.");
                    }
                }
            }
        }.updateWhen(updateDuring);
    }

    public void createCollector(FloatOutput collectorMotor, FloatInputPoll speed, BooleanOutput openFingers,
            final BooleanInputPoll rollersIn, final BooleanInputPoll rollersOut, BooleanInputPoll disableCollector, BooleanInputPoll overrideRoll) {
        during.addListener(Mixing.filterEvent(disableCollector, true, Mixing.filterEvent(rollersIn, true, new EventConsumer() {
            public void eventFired() {
                ErrorMessages.displayError(2, "Collect: not winch!", 200);
            }
        })));
        Mixing.pumpWhen(during, Mixing.quadSelect(Mixing.orBooleans(rollersIn, overrideRoll), rollersOut,
                Mixing.always(0f), Mixing.negate(speed),
                Mixing.select(disableCollector, speed, Mixing.always(0)), speed),
                collectorMotor);
        Mixing.pumpWhen(during, Mixing.orBooleans(rollersIn, rollersOut), openFingers);
    }
}
