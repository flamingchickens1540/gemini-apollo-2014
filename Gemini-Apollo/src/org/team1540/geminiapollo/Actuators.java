package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.TuningContext;

public class Actuators {

    private final EventSource during;
    public final EventConsumer armUp, armDown, armFloat;
    private BooleanOutput armBackward, armForward;
    private final BooleanStatus runArmPositioner = new BooleanStatus();
    private final TuningContext armTuning = new TuningContext(CluckGlobals.getNode(), "arm-tuning").publishSavingEvent("Arm Tuning");
    private final FloatStatus armLow = armTuning.getFloat("arm-low", 0f);
    private final FloatStatus armHigh = armTuning.getFloat("arm-high", 10f);

    public Actuators(EventSource updateDuring) {
        this.during = updateDuring;
        EventConsumer reset = Mixing.getSetEvent(Mixing.combine(armBackward, armForward, runArmPositioner), false);
        armUp = Mixing.combine(reset, Mixing.getSetEvent(armBackward, true));
        armDown = Mixing.combine(reset, Mixing.getSetEvent(armForward, true));
        armFloat = Mixing.combine(reset, Mixing.getSetEvent(runArmPositioner, true));
    }

    public void createCollector(FloatOutput collectorMotor, FloatInputPoll speed, BooleanOutput armFloatSolenoid,
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
        Mixing.pumpWhen(during, Mixing.orBooleans(rollersIn, rollersOut), armFloatSolenoid);
    }

    public void createArm(BooleanOutput armSolenoidForward, BooleanOutput armSolenoidBackward, final FloatInputPoll active) {
        this.armForward = armSolenoidForward;
        this.armBackward = armSolenoidBackward;
        during.addListener(Mixing.filterEvent(runArmPositioner, true, new EventConsumer() {
            public void eventFired() {
                float val = active.readValue();
                armBackward.writeValue(val < armLow.readValue());
                armForward.writeValue(val > armHigh.readValue());
            }
        }));
    }
}
