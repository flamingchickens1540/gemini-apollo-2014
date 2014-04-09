package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.TuningContext;

public class Actuators {

    private final EventSource during;
    public final EventConsumer armUp, armDown, armAlign, armFloat;
    private BooleanOutput armBackward, armForward, showArmRaise, showArmLower;
    private final BooleanStatus runArmPositioner = new BooleanStatus();
    private final TuningContext armTuning = new TuningContext(CluckGlobals.getNode(), "ArmTuning").publishSavingEvent("Arm Tuning");
    private final FloatStatus armTooLowered = armTuning.getFloat("arm-too-lowered", 0f);
    private final FloatStatus armTooRaised = armTuning.getFloat("arm-too-raised", 10f);
    private final BooleanStatus collectorRequiresFloat = new BooleanStatus();

    private void reset() {
        armBackward.writeValue(false);
        armForward.writeValue(false);
        runArmPositioner.writeValue(false);
        showArmLower.writeValue(true);
        showArmRaise.writeValue(true);
    }
    
    public Actuators(EventSource updateDuring, final BooleanOutput isSafeToShoot, final BooleanOutput isArmLower, final BooleanOutput isArmRaise) {
        this.during = updateDuring;
        showArmLower = isArmLower;
        showArmRaise = isArmRaise;
        armUp = new EventConsumer() {
            public void eventFired() {
                reset();
                if (!collectorRequiresFloat.readValue()) {
                    armBackward.writeValue(true);
                    isSafeToShoot.writeValue(false);
                    isArmRaise.writeValue(false);
                }
            }
        };
        armDown = new EventConsumer() {
            public void eventFired() {
                reset();
                if (!collectorRequiresFloat.readValue()) {
                    armForward.writeValue(true);
                    isSafeToShoot.writeValue(true);
                    isArmLower.writeValue(false);
                }
            }
        };
        armAlign = new EventConsumer() {
            public void eventFired() {
                reset();
                if (!collectorRequiresFloat.readValue()) {
                    runArmPositioner.writeValue(true);
                    isSafeToShoot.writeValue(true);
                    isArmRaise.writeValue(false);
                    isArmLower.writeValue(false);
                }
            }
        };
        armFloat = new EventConsumer() {
            public void eventFired() {
                reset();
            }
        };
        Mixing.whenBooleanBecomes(collectorRequiresFloat, true).addListener(armFloat);
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
        Mixing.pumpWhen(during, Mixing.orBooleans(rollersIn, rollersOut), Mixing.combine(openFingers, collectorRequiresFloat));
    }

    public void createArm(BooleanOutput armSolenoidForward, BooleanOutput armSolenoidBackward, final FloatInputPoll active) {
        this.armForward = armSolenoidForward;
        this.armBackward = armSolenoidBackward;
        during.addListener(Mixing.filterEvent(runArmPositioner, true, new EventConsumer() {
            public void eventFired() {
                float val = active.readValue();
                armBackward.writeValue(val < armTooRaised.readValue());
                armForward.writeValue(val > armTooLowered.readValue());
            }
        }));
    }
}
