package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.cluck.tcp.CluckTCPServer;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.TuningContext;
import ccre.igneous.SimpleCore;
import ccre.log.Logger;

public class RobotMain extends SimpleCore {

    public static final boolean IS_COMPETITION_ROBOT = true;
    private TestMode testing;
    private ControlInterface ui;

    private void setupCluck() {
        new CluckTCPServer(CluckGlobals.getNode(), 443).start();
        new CluckTCPServer(CluckGlobals.getNode(), 1180).start();
        new CluckTCPServer(CluckGlobals.getNode(), 1540).start();
        new CluckTCPServer(CluckGlobals.getNode(), 1735).start();
    }

    protected void createSimpleControl() {
        ui = new ControlInterface(joystick1, joystick2);
        ErrorMessages.setupError(constantPeriodic);
        setupCluck();
        testing = new TestMode(getIsTest());
        // ***** MOTORS *****
        FloatOutput leftDrive1 = makeTalonMotor(1, MOTOR_FORWARD, 0.1f), rightDrive1 = makeTalonMotor(3, MOTOR_REVERSE, 0.1f);
        FloatOutput leftDrive2 = makeTalonMotor(2, MOTOR_FORWARD, 0.1f), rightDrive2 = makeTalonMotor(4, MOTOR_REVERSE, 0.1f);
        FloatOutput leftDrive = Mixing.combine(leftDrive1, leftDrive2), rightDrive = Mixing.combine(rightDrive1, rightDrive2);
        Mixing.setWhen(robotDisabled, Mixing.combine(leftDrive, rightDrive), 0);
        testing.addDriveMotors(leftDrive1, leftDrive2, leftDrive, rightDrive1, rightDrive2, rightDrive);
        FloatOutput winchMotor = testing.testPublish("winch", makeTalonMotor(5, MOTOR_REVERSE, 1000f));
        FloatOutput collectorMotor = testing.testPublish("collectorMotor", makeTalonMotor(6, MOTOR_REVERSE, 0.1f));
        // ***** SOLENOIDS *****
        BooleanOutput shiftSolenoid = testing.testPublish("sol-shift-1", makeSolenoid(1));
        BooleanStatus armSolenoid = new BooleanStatus(testing.testPublish("sol-arm-2", makeSolenoid(2)));
        Mixing.setWhen(robotDisabled, armSolenoid, false);
        BooleanOutput winchSolenoid = testing.testPublish("sol-winch-3", makeSolenoid(3));
        BooleanOutput openFingers = testing.testPublish("sol-open-5", makeSolenoid(5));
        CluckGlobals.getNode().publish("Finger Override", openFingers);
        BooleanOutput armFloat = testing.testPublish("sol-float-6", makeSolenoid(6));
        BooleanOutput collectionSolenoids = Mixing.combine(new BooleanStatus(openFingers), armFloat);
        // ***** INPUTS *****
        final FloatInputPoll winchCurrent = makeAnalogInput(1, 8);
        final BooleanInputPoll catapultNotCocked = makeDigitalInput(2);
        CluckGlobals.getNode().publish("Winch Current", Mixing.createDispatch(winchCurrent, globalPeriodic));
        CluckGlobals.getNode().publish("Catapult Not Cocked", Mixing.createDispatch(catapultNotCocked, globalPeriodic));
        setupCompressor(winchCurrent);
        // ***** CONTROL INTERFACE *****
        BooleanInput armShouldBeDown = ui.getArmShouldBeDown(robotDisabled);
        BooleanInput rearmButton = ui.getRearmCatapult(globalPeriodic);
        // [[[[ AUTONOMOUS CODE ]]]]
        AutonomousController instinct = new AutonomousController(this);
        instinct.putDriveMotors(leftDrive, rightDrive);
        instinct.putKinectTrigger(KinectControl.main(globalPeriodic,
                makeDispatchJoystick(5, globalPeriodic), makeDispatchJoystick(6, globalPeriodic)));
        instinct.putArm(armSolenoid, collectorMotor, collectionSolenoids);
        EventSource fireAutonomousTrigger = instinct.getWhenToFire(), rearmAutonomousTrigger = instinct.getWhenToRearm();
        EventConsumer notifyRearmFinished = instinct.getNotifyRearmFinished();
        // [[[[ DRIVE CODE ]]]]
        BooleanStatus shiftBoolean = DriveCode.createShifting(startedTeleop, startedAutonomous, duringTeleop,
                shiftSolenoid, ui.getShiftHighButton(), ui.getShiftLowButton());
        DriveCode.createDrive(startedTeleop, duringTeleop, leftDrive, rightDrive,
                ui.getLeftDriveAxis(), ui.getRightDriveAxis(), ui.getForwardDriveAxis(), shiftBoolean);
        // [[[[ SHOOTER CODE ]]]]
        EventSource fireWhen = Mixing.combine(fireAutonomousTrigger, ui.getFireButton());
        FloatInputPoll voltage = getBatteryVoltage();
        CluckGlobals.getNode().publish("Battery Level", Mixing.createDispatch(voltage, globalPeriodic));
        Shooter shooter = new Shooter(robotDisabled, Mixing.filterEvent(getIsTest(), false, globalPeriodic), constantPeriodic, Mixing.orBooleans(armShouldBeDown, getIsAutonomous()), voltage);
        EventSource rearmEvent = Mixing.whenBooleanBecomes(rearmButton, true);
        shooter.setupWinch(winchMotor, winchSolenoid, winchCurrent, rearmButton);
        shooter.setupRearmTimeout();
        shooter.handleShooterButtons(
                Mixing.invert(catapultNotCocked),
                Mixing.combine(rearmAutonomousTrigger, rearmEvent), fireWhen,
                notifyRearmFinished
        );
        shooter.createTuner(winchCurrent, rearmEvent, catapultNotCocked);
        BooleanStatus forceRunCollectorForArmAutolower = new BooleanStatus();
        shooter.setupArmLower(ui.forceArmLower(), forceRunCollectorForArmAutolower);
        // [[[[ ARM CODE ]]]]
        Actuators act = new Actuators(duringTeleop);
        act.createArm(armSolenoid, armShouldBeDown, IS_COMPETITION_ROBOT ? Mixing.alwaysFalse : shooter.rearming);
        act.createCollector(collectorMotor, ui.collectorSpeed(), collectionSolenoids,
                ui.rollerIn(), ui.rollerOut(), shooter.winchDisengaged, forceRunCollectorForArmAutolower);
        // [[[[ Phidget Display Code ]]]]
        ui.showFiring(globalPeriodic, shooter.winchDisengaged);
        ui.showArm(armShouldBeDown);
        duringTeleop.addListener(new EventConsumer() {
            public void eventFired() {
                ErrorMessages.displayError(0, "(1540) APOLLO (TELE)", 200);
            }
        });
        duringAutonomous.addListener(new EventConsumer() {
            public void eventFired() {
                ErrorMessages.displayError(0, "(AUTO) APOLLO (1540)", 200);
            }
        });
        duringTesting.addListener(new EventConsumer() {
            public void eventFired() {
                ErrorMessages.displayError(0, "(TEST) APOLLO (TEST)", 200);
            }
        });
    }

    private void setupCompressor(FloatInputPoll winch) {
        final BooleanInputPoll pressureSwitch = makeDigitalInput(1);
        final FloatStatus override = new FloatStatus();
        final FloatInputPoll pressureSensor = makeAnalogInput(2, 8);
        CluckGlobals.getNode().publish("Compressor Override", override);
        CluckGlobals.getNode().publish("Compressor Sensor", Mixing.createDispatch(pressureSwitch, globalPeriodic));
        CluckGlobals.getNode().publish("Pressure Sensor", Mixing.createDispatch(pressureSensor, globalPeriodic));
        final TuningContext tuner = new TuningContext(CluckGlobals.getNode(), "PressureTuner");
        tuner.publishSavingEvent("Pressure");
        final FloatInputPoll zeroP = tuner.getFloat("LowPressure", 0.494f); // 0.5
        final FloatInputPoll oneP = tuner.getFloat("HighPressure", 2.746f); // 2.745
        final FloatInputPoll percentPressure = new FloatInputPoll() {
            public float readValue() {
                return 100 * ControlInterface.normalize(zeroP.readValue(), oneP.readValue(), pressureSensor.readValue());
            }
        };
        if (ui == null) {
            Logger.warning("UI not initialized!");
        }
        ui.displayPressureAndWinch(percentPressure, globalPeriodic, pressureSwitch, winch);
        constantPeriodic.addListener(new EventConsumer() {
            public void eventFired() {
                float value = override.readValue();
                if (value > 0) {
                    override.writeValue(Math.max(0, Math.min(10, value) - 0.01f));
                }
            }
        });
        useCustomCompressor(new BooleanInputPoll() {
            public boolean readValue() {
                float value = override.readValue();
                return value < 0 || (pressureSwitch.readValue() && value == 0) || percentPressure.readValue() > 105;
            }
        }, 1);
    }
}
