package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.cluck.tcp.CluckTCPServer;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.holders.TuningContext;
import ccre.igneous.SimpleCore;

public class RobotMain extends SimpleCore {

    public static final boolean IS_COMPETITION_ROBOT = true;
    private TestMode testing;
    private ControlInterface ui;

    protected void createSimpleControl() {
        ui = new ControlInterface(joystick1, joystick2, globalPeriodic, robotDisabled);
        ErrorMessages.setupError(constantPeriodic);
        new CluckTCPServer(CluckGlobals.getNode(), 443).start();
        new CluckTCPServer(CluckGlobals.getNode(), 1180).start();
        new CluckTCPServer(CluckGlobals.getNode(), 1540).start();
        testing = new TestMode(getIsTest());
        AutonomousController autonomous = new AutonomousController(this);

        FloatInputPoll voltage = getBatteryVoltage();
        CluckGlobals.getNode().publish("Battery Level", Mixing.createDispatch(voltage, globalPeriodic));
        { // ==== DRIVING ====
            FloatOutput leftDrive1 = makeTalonMotor(1, MOTOR_FORWARD, 0.1f), rightDrive1 = makeTalonMotor(3, MOTOR_REVERSE, 0.1f);
            FloatOutput leftDrive2 = makeTalonMotor(2, MOTOR_FORWARD, 0.1f), rightDrive2 = makeTalonMotor(4, MOTOR_REVERSE, 0.1f);
            FloatOutput leftDrive = Mixing.combine(leftDrive1, leftDrive2), rightDrive = Mixing.combine(rightDrive1, rightDrive2);
            BooleanOutput shiftSolenoid = testing.testPublish("sol-shift-1", makeSolenoid(1));
            testing.addDriveMotors(leftDrive1, leftDrive2, leftDrive, rightDrive1, rightDrive2, rightDrive);
            // Reset
            Mixing.setWhen(robotDisabled, Mixing.combine(leftDrive, rightDrive), 0);
            // Teleoperated
            BooleanStatus shiftBoolean = DriveCode.createShifting(startedTeleop, startedAutonomous, duringTeleop,
                    shiftSolenoid, ui.getShiftHighButton(), ui.getShiftLowButton());
            DriveCode.createDrive(startedTeleop, duringTeleop, leftDrive, rightDrive,
                    ui.getLeftDriveAxis(), ui.getRightDriveAxis(), ui.getForwardDriveAxis(), shiftBoolean);
            // Autonomous
            autonomous.putDriveMotors(leftDrive, rightDrive);
        }
        FloatInputPoll displayReading;
        BooleanStatus safeToShoot = new BooleanStatus(), forceRunCollectorForArmAutolower = new BooleanStatus();
        BooleanInputPoll safeToCollect;
        { // ==== SHOOTER CODE ====
            FloatOutput winchMotor = testing.testPublish("winch", makeTalonMotor(5, MOTOR_REVERSE, 1000f));
            BooleanOutput winchSolenoid = testing.testPublish("sol-winch-3", makeSolenoid(3));
            FloatInputPoll winchCurrent = makeAnalogInput(1, 8);
            CluckGlobals.getNode().publish("Winch Current", Mixing.createDispatch(winchCurrent, globalPeriodic));
            EventSource fireWhen = Mixing.combine(autonomous.getWhenToFire(), ui.getFireButton());
            // Global
            Shooter shooter = new Shooter(robotDisabled, Mixing.filterEvent(getIsTest(), false, globalPeriodic),
                    constantPeriodic, Mixing.orBooleans(safeToShoot, getIsAutonomous()), voltage);
            EventSource rearmEvent = ui.getRearmCatapult();
            shooter.setupWinch(winchMotor, winchSolenoid, winchCurrent);
            // Teleop
            shooter.setupRearmTimeout();
            shooter.handleShooterButtons(
                    Mixing.combine(autonomous.getWhenToRearm(), rearmEvent),
                    fireWhen, autonomous.getNotifyRearmFinished());
            shooter.setupArmLower(ui.forceArmLower(), forceRunCollectorForArmAutolower);
            safeToCollect = shooter.winchDisengaged;
            // Autonomous
            autonomous.putCurrentActivator(shooter.shouldUseCurrent);
            // Readouts
            displayReading = Mixing.select(shooter.shouldUseCurrent, shooter.totalPowerTaken, shooter.activeAmps);
            ui.showFiring(shooter.winchDisengaged);
        }
        { // ==== ARM CODE ====
            BooleanOutput armMainSolenoid = testing.testPublish("sol-arm-2", makeSolenoid(2));
            BooleanOutput armLockSolenoid = testing.testPublish("sol-float-6", makeSolenoid(6));
            BooleanOutput collectionSolenoids = Mixing.combine(Mixing.invert(testing.testPublish("sol-fingers-5", makeSolenoid(5))), testing.testPublish("sol-lock-4", makeSolenoid(4)));
            collectionSolenoids.writeValue(false);
            FloatOutput collectorMotor = testing.testPublish("collectorMotor", makeTalonMotor(6, MOTOR_REVERSE, 0.1f));
            // Teleoperated
            Actuators act = new Actuators(Mixing.orBooleans(getIsTeleop(), getIsAutonomous()), globalPeriodic, safeToShoot, ui.showArmDown(), ui.showArmUp(), armMainSolenoid, armLockSolenoid);
            robotDisabled.addListener(act.armUp);
            ui.getArmLower().addListener(act.armDown);
            ui.getArmRaise().addListener(act.armUp);
            ui.getArmHold().addListener(act.armAlign);
            CluckGlobals.getNode().publish("Arm Align", act.armAlign);
            act.createCollector(collectorMotor, ui.collectorSpeed(), collectionSolenoids,
                    ui.rollerIn(), ui.rollerOut(), safeToCollect, Mixing.orBooleans(forceRunCollectorForArmAutolower, ui.shouldBeCollectingBecauseLoader()));
            // Autonomous
            autonomous.putArm(act.armUp, act.armDown, collectorMotor, collectionSolenoids);
        }
        { // ==== KINECT CODE
            autonomous.putKinectTrigger(KinectControl.main(globalPeriodic,
                    makeDispatchJoystick(5, globalPeriodic), makeDispatchJoystick(6, globalPeriodic)));
        }
        // ==== Compressor ====
        setupCompressorAndDisplay(displayReading);
        // ==== Phidget Mode Code ====
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

    private void setupCompressorAndDisplay(FloatInputPoll winch) {
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
