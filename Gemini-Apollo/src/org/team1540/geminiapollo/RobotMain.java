package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.cluck.tcp.CluckTCPServer;
import ccre.ctrl.Mixing;
import ccre.ctrl.MultipleSourceBooleanController;
import ccre.event.*;
import ccre.igneous.SimpleCore;

public class RobotMain extends SimpleCore {

    public static boolean IS_COMPETITION_ROBOT = false;
    private TestMode testing;
    private ControlInterface ui;

    private void setupCluck() {
        new CluckTCPServer(CluckGlobals.node, 443).start();
        new CluckTCPServer(CluckGlobals.node, 1180).start();
        new CluckTCPServer(CluckGlobals.node, 1540).start();
        new CluckTCPServer(CluckGlobals.node, 1735).start();
    }

    protected void createSimpleControl() {
        ui = new ControlInterface(joystick1, joystick2);
        setupCluck();
        setupCompressor();
        testing = new TestMode(getIsTest());
        // ***** MOTORS *****
        FloatOutput leftDrive1 = makeTalonMotor(1, MOTOR_FORWARD, 0.1f), rightDrive1 = makeTalonMotor(3, MOTOR_REVERSE, 0.1f);
        FloatOutput leftDrive2 = makeTalonMotor(2, MOTOR_FORWARD, 0.1f), rightDrive2 = makeTalonMotor(4, MOTOR_REVERSE, 0.1f);
        FloatOutput leftDrive = Mixing.combine(leftDrive1, leftDrive2), rightDrive = Mixing.combine(rightDrive1, rightDrive2);
        Mixing.setWhen(robotDisabled, Mixing.combine(leftDrive, rightDrive), 0);
        testing.addDriveMotors(leftDrive1, leftDrive2, leftDrive, rightDrive1, rightDrive2, rightDrive);
        FloatOutput winchMotor = testing.testPublish("winch", makeTalonMotor(5, MOTOR_REVERSE, 0.1f));
        FloatOutput collectorMotor = testing.testPublish("collectorMotor", makeTalonMotor(6, MOTOR_REVERSE, 0.1f));
        // ***** SOLENOIDS *****
        BooleanOutput shiftSolenoid = testing.testPublish("sol-shift-1", makeSolenoid(1));
        MultipleSourceBooleanController armSolenoidCtrl = new MultipleSourceBooleanController(true);
        armSolenoidCtrl.addTarget(testing.testPublish("sol-arm-2", makeSolenoid(2)));
        BooleanStatus armSolenoid = new BooleanStatus(armSolenoidCtrl.getOutput(false));
        Mixing.setWhen(robotDisabled, armSolenoid, false);
        BooleanOutput winchSolenoid = testing.testPublish("sol-winch-3", makeSolenoid(3));
        BooleanOutput openFingers = testing.testPublish("sol-open-5", makeSolenoid(5));
        BooleanOutput armFloat = testing.testPublish("sol-float-6", makeSolenoid(6));
        BooleanOutput armFloatSolenoid = Mixing.combine(openFingers, armFloat);
        // ***** INPUTS *****
        final FloatInputPoll winchCurrent = makeAnalogInput(1, 8);
        final BooleanInputPoll catapultNotCocked = makeDigitalInput(2);
        CluckGlobals.node.publish("Winch Current", Mixing.createDispatch(winchCurrent, globalPeriodic));
        CluckGlobals.node.publish("Catapult Not Cocked", Mixing.createDispatch(catapultNotCocked, globalPeriodic));
        // ***** CONTROL INTERFACE *****
        BooleanInput armShouldBeDown = ui.getArmShouldBeDown();
        BooleanInput rearmButton = ui.getRearmCatapult(globalPeriodic);
        // ***** KINECT CODE *****
        BooleanInputPoll fireAuto = KinectControl.main(
                makeDispatchJoystick(5, globalPeriodic), makeDispatchJoystick(6, globalPeriodic), globalPeriodic);
        // [[[[ AUTONOMOUS CODE ]]]]
        AutonomousController instinct = new AutonomousController(this);
        instinct.putDriveMotors(leftDrive, rightDrive);
        instinct.putKinectTrigger(fireAuto);
        instinct.putArm(armSolenoid, collectorMotor);
        EventSource fireAutonomousTrigger = instinct.getWhenToFire(), rearmAutonomousTrigger = instinct.getWhenToRearm();
        EventConsumer notifyRearmFinished = instinct.getNotifyRearmFinished();
        // [[[[ DRIVE CODE ]]]]
        BooleanStatus shiftBoolean = DriveCode.createShifting(startedTeleop, startedAutonomous, duringTeleop,
                shiftSolenoid, ui.getShiftHighButton(), ui.getShiftLowButton());
        DriveCode.createDrive(startedTeleop, duringTeleop, leftDrive, rightDrive,
                ui.getLeftDriveAxis(), ui.getRightDriveAxis(), ui.getForwardDriveAxis(), shiftBoolean);
        // [[[[ SHOOTER CODE ]]]]
        EventSource fireWhen = Mixing.combine(fireAutonomousTrigger, ui.getFireButton());
        Shooter shooter = new Shooter(robotDisabled, globalPeriodic, constantPeriodic);
        EventSource rearmEvent = Mixing.whenBooleanBecomes(rearmButton, true);
        shooter.setupWinch(winchMotor, winchSolenoid, winchCurrent, rearmButton);
        shooter.handleShooterButtons(
                Mixing.invert(catapultNotCocked), Mixing.orBooleans(armShouldBeDown, getIsAutonomous()),
                Mixing.combine(rearmAutonomousTrigger, rearmEvent), fireWhen,
                notifyRearmFinished
        );
        shooter.createTuner(winchCurrent, rearmEvent, catapultNotCocked);
        // [[[[ ARM CODE ]]]]
        Actuators act = new Actuators(duringTeleop);
        act.createArm(armSolenoid, armShouldBeDown, IS_COMPETITION_ROBOT ? Mixing.alwaysFalse : shooter.rearming);
        act.createCollector(collectorMotor, ui.collectorSpeed(), armFloatSolenoid, ui.rollerIn(), ui.rollerOut(), shooter.winchDisengaged);
        // [[[[ Phidget Display Code ]]]]
        ui.showFiring(globalPeriodic, shooter.winchDisengaged);
        ui.showArm(armShouldBeDown);
    }

    private void setupCompressor() {
        final BooleanInputPoll pressureSwitch = makeDigitalInput(1);
        final FloatStatus override = new FloatStatus();
        final FloatInputPoll pressureSensor = makeAnalogInput(2, 8);
        CluckGlobals.node.publish("Compressor Override", override);
        CluckGlobals.node.publish("Compressor Sensor", Mixing.createDispatch(pressureSwitch, globalPeriodic));
        CluckGlobals.node.publish("Pressure Sensor", Mixing.createDispatch(pressureSensor, globalPeriodic));
        ui.displayPressure(pressureSensor, globalPeriodic, pressureSwitch);
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
                return value < 0 || (pressureSwitch.readValue() && value == 0);
            }
        }, 1);
    }
}
