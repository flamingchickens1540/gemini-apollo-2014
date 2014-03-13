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
    private ControlInterface ctrlif;

    private void setupCluck() {
        new CluckTCPServer(CluckGlobals.node, 443).start();
        new CluckTCPServer(CluckGlobals.node, 1180).start();
        new CluckTCPServer(CluckGlobals.node, 1540).start();
        new CluckTCPServer(CluckGlobals.node, 1735).start();
    }

    protected void createSimpleControl() {
        ctrlif = new ControlInterface(joystick2);
        setupCluck();
        testing = new TestMode(getIsTest());
        // ***** MOTORS *****
        FloatOutput leftDrive1 = testing.testPublish("leftDrive1", makeTalonMotor(1, MOTOR_FORWARD, 0.1f));
        FloatOutput leftDrive2 = testing.testPublish("leftDrive2", makeTalonMotor(2, MOTOR_FORWARD, 0.1f));
        testing.testPublish("leftDrive", Mixing.combine(leftDrive1, leftDrive2));
        FloatOutput rightDrive1 = testing.testPublish("rightDrive1", makeTalonMotor(3, MOTOR_REVERSE, 0.1f));
        FloatOutput rightDrive2 = testing.testPublish("rightDrive2", makeTalonMotor(4, MOTOR_REVERSE, 0.1f));
        testing.testPublish("rightDrive", Mixing.combine(rightDrive1, rightDrive2));
        FloatOutput winchMotor = testing.testPublish("winch", makeTalonMotor(5, MOTOR_REVERSE, 0.1f));
        FloatOutput collectorMotor = testing.testPublish("collectorMotor", makeTalonMotor(6, MOTOR_REVERSE, 0.1f));
        // ***** SOLENOIDS *****
        BooleanOutput shiftSolenoid = testing.testPublish("sol-shift-1", makeSolenoid(1));
        MultipleSourceBooleanController armSolenoidCtrl = new MultipleSourceBooleanController(true);
        armSolenoidCtrl.addTarget(testing.testPublish("sol-arm-2", makeSolenoid(2)));
        BooleanStatus armSolenoid = new BooleanStatus(armSolenoidCtrl.getOutput(false));
        Mixing.setWhen(robotDisabled, armSolenoid, false);
        BooleanOutput winchSolenoidA = testing.testPublish("sol-winch-3", makeSolenoid(3));
        BooleanOutput winchSolenoidB = testing.testPublish("sol-winch-5", makeSolenoid(5));
        BooleanOutput winchSolenoid = testing.testPublish("sol-winch-combo", Mixing.combine(winchSolenoidA, winchSolenoidB));
        //BooleanOutput rachetLoopRelease = test.testPublish("sol-rachet-5", makeSolenoid(5));
        BooleanOutput armFloatSolenoid = testing.testPublish("sol-float-6", makeSolenoid(6));
        // ***** INPUTS *****
        final FloatInputPoll winchCurrent = makeAnalogInput(1, 8);
        final FloatInputPoll pressureSensor = makeAnalogInput(2, 8);
        final BooleanInputPoll catapultNotCocked = makeDigitalInput(2);
        CluckGlobals.node.publish("Winch Current", Mixing.createDispatch(winchCurrent, globalPeriodic));
        CluckGlobals.node.publish("Pressure Sensor", Mixing.createDispatch(pressureSensor, globalPeriodic));
        CluckGlobals.node.publish("Catapult Not Cocked", Mixing.createDispatch(catapultNotCocked, globalPeriodic));
        // ***** COMPRESSOR *****
        BooleanInputPoll pressureSwitch = makeDigitalInput(1);
        useCustomCompressor(Actuators.calcCompressorControl(constantPeriodic, pressureSwitch), 1);
        // ***** CONTROL INTERFACE *****
        BooleanInput armUpDown = ControlInterface.getArmUpDown();
        BooleanInputPoll rollersIn = ControlInterface.rollerIn();
        BooleanInputPoll rollersOut = ControlInterface.rollerOut();
        BooleanInput detensioning = ControlInterface.detensioning();
        BooleanInput rearmButton = ControlInterface.getRearmCatapult(globalPeriodic);
        EventSource fireButton = ControlInterface.getFireButton();
        // ***** DRIVE JOYSTICK *****
        FloatInputPoll leftDriveAxis = Mixing.negate(joystick1.getAxisChannel(2));
        FloatInputPoll forwardDriveAxis = Mixing.negate(joystick1.getAxisChannel(3));
        FloatInputPoll rightDriveAxis = Mixing.negate(joystick1.getAxisChannel(5));
        EventSource shiftHighButton = joystick1.getButtonSource(1);
        EventSource shiftLowButton = joystick1.getButtonSource(3);
        // ***** KINECT CODE *****
        BooleanInputPoll fireAuto = KinectControl.main(
                makeDispatchJoystick(5, globalPeriodic), makeDispatchJoystick(6, globalPeriodic), globalPeriodic);
        // [[[[ USER AUTOMATION CODE ]]]]
        BooleanInputPoll overrideCollectorBackwards = UserAutomation.setupAuto(
                Mixing.whenBooleanBecomes(detensioning, true),
                Mixing.invert(armSolenoidCtrl.getOutput(true)));
        // [[[[ AUTONOMOUS CODE ]]]]
        AutonomousController controller = new AutonomousController();
        controller.setup(this);
        controller.putDriveMotors(leftDrive1, leftDrive2, rightDrive1, rightDrive2);
        controller.putKinectTrigger(fireAuto);
        controller.putArm(armSolenoid, collectorMotor);
        EventSource fireAutonomousTrigger = controller.getWhenToFire(), rearmAutonomousTrigger = controller.getWhenToRearm();
        EventConsumer notifyRearmFinished = controller.getNotifyRearmFinished();
        // [[[[ DRIVE CODE ]]]]
        BooleanStatus shiftBoolean = DriveCode.createShifting(startedTeleop, startedAutonomous, duringTeleop, shiftSolenoid, shiftHighButton, shiftLowButton);
        DriveCode.createDrive(startedTeleop, duringTeleop, leftDrive1, leftDrive2, rightDrive1, rightDrive2, leftDriveAxis, rightDriveAxis, forwardDriveAxis, IS_COMPETITION_ROBOT, shiftBoolean);
        // [[[[ SHOOTER CODE ]]]]
        EventSource fireWhen = Mixing.combine(fireAutonomousTrigger, fireButton);
        EventSource updateShooterWhen = Mixing.combine(duringTeleop, duringAutonomous);
        BooleanStatus canCollectorRun = new BooleanStatus();
        BooleanStatus winchEngaged = new BooleanStatus(winchSolenoid);
        BooleanInputPoll rearming = Shooter.createShooter(
                startedAutonomous, startedTeleop, updateShooterWhen, constantPeriodic,
                getIsAutonomous(),
                winchMotor,
                winchSolenoid,
                winchCurrent,
                catapultNotCocked, armUpDown,
                rearmButton, rearmAutonomousTrigger, fireWhen, canCollectorRun,
                winchEngaged, notifyRearmFinished
        );
        Shooter.createTuner(globalPeriodic, winchCurrent, Mixing.whenBooleanBecomes(rearmButton, true), catapultNotCocked);
        // [[[[ ARM CODE ]]]]
        Actuators.createArm(duringTeleop, armSolenoid, armUpDown, IS_COMPETITION_ROBOT ? Mixing.alwaysTrue : rearming);
        Actuators.createCollector(duringTeleop, collectorMotor, ControlInterface.collectorSpeed(),
                armFloatSolenoid, Mixing.orBooleans(rollersIn, overrideCollectorBackwards), rollersOut, canCollectorRun);
        // [[[[ Phidget Display Code ]]]]
        ControlInterface.displayPressure(pressureSensor, globalPeriodic, pressureSwitch);
        ControlInterface.showFiring(globalPeriodic, winchEngaged);
        ControlInterface.showArm(armUpDown);
        //MOTD.createMOTD();
    }
}
