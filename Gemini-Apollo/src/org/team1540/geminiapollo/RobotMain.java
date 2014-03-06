package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.cluck.tcp.CluckTCPServer;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.igneous.SimpleCore;
import ccre.log.*;

public class RobotMain extends SimpleCore {

    public static boolean IS_COMPETITION_ROBOT = true;

    protected void createSimpleControl() {
        // ***** CLUCK *****
        new CluckTCPServer(CluckGlobals.node, 443).start();
        new CluckTCPServer(CluckGlobals.node, 1180).start();
        new CluckTCPServer(CluckGlobals.node, 1540).start();
        new CluckTCPServer(CluckGlobals.node, 1735).start();
        TestMode test = new TestMode(getIsTest());
        // ***** MOTORS *****
        FloatOutput leftDrive1 = test.testPublish("leftDrive1", makeTalonMotor(1, MOTOR_FORWARD, 0.1f));
        FloatOutput leftDrive2 = test.testPublish("leftDrive2", makeTalonMotor(2, MOTOR_FORWARD, 0.1f));
        test.testPublish("leftDrive", Mixing.combine(leftDrive1, leftDrive2));
        FloatOutput rightDrive1 = test.testPublish("rightDrive1", makeTalonMotor(3, MOTOR_REVERSE, 0.1f));
        FloatOutput rightDrive2 = test.testPublish("rightDrive2", makeTalonMotor(4, MOTOR_REVERSE, 0.1f));
        test.testPublish("rightDrive", Mixing.combine(rightDrive1, rightDrive2));
        FloatOutput winchMotor = test.testPublish("winch", makeTalonMotor(5, MOTOR_REVERSE, 0.1f));
        FloatOutput collectorMotor = test.testPublish("collectorMotor", makeTalonMotor(6, MOTOR_REVERSE, 0.1f));
        // ***** SOLENOIDS *****
        BooleanOutput shiftSolenoid = test.testPublish("sol-shift-1", makeSolenoid(1));
        BooleanOutput armSolenoid = test.testPublish("sol-arm-2", makeSolenoid(2));
        Mixing.setWhen(robotDisabled, armSolenoid, false);
        BooleanOutput winchSolenoidA = test.testPublish("sol-winch-3", makeSolenoid(3));
        BooleanOutput winchSolenoidB = test.testPublish("sol-winch-5", makeSolenoid(5));
        BooleanOutput winchSolenoid = test.testPublish("sol-winch-combo", Mixing.combine(winchSolenoidA,winchSolenoidB));
        //BooleanOutput rachetLoopRelease = test.testPublish("sol-rachet-5", makeSolenoid(5));
        BooleanOutput armFloatSolenoid = test.testPublish("sol-float-6", makeSolenoid(6));
        // ***** INPUTS *****
        final FloatInputPoll winchCurrent = makeAnalogInput(1, 8);
        final FloatInputPoll pressureSensor = makeAnalogInput(2, 8);
        final FloatInputPoll ultrasonicSensor = makeAnalogInput(3, 8);
        final BooleanInputPoll catapultNotCocked = makeDigitalInput(2);
        CluckGlobals.node.publish("Winch Current", Mixing.createDispatch(winchCurrent, globalPeriodic));
        CluckGlobals.node.publish("Pressure Sensor", Mixing.createDispatch(pressureSensor, globalPeriodic));
        CluckGlobals.node.publish("Ultrasonic Sensor", Mixing.createDispatch(ultrasonicSensor, globalPeriodic));
        FloatInput distance = Mixing.createDispatch(new FloatInputPoll(){
            public float readValue() {
                return ultrasonicSensor.readValue()*(1024/5f) - 4;
            }
        },globalPeriodic);
        CluckGlobals.node.publish("Ultrasonic Sensor, centimenters", distance);
        // ***** VISION TRACKING *****
        VisionTracking.setup(startedAutonomous);
        BooleanInputPoll isHotZone = VisionTracking.isHotZone();
        // ***** COMPRESSOR *****
        BooleanInputPoll pressureSwitch = makeDigitalInput(1);
        useCustomCompressor(Actuators.calcCompressorControl(constantPeriodic, pressureSwitch), 1);
        // ***** CONTROL INTERFACE *****
        BooleanInputPoll armUpDown = ControlInterface.getArmUpDown();
        BooleanInputPoll rollersIn = ControlInterface.rollerIn();
        BooleanInputPoll rollersOut = ControlInterface.rollerOut();
        BooleanInputPoll detensioning = ControlInterface.detensioning();
        EventSource rearmCatapult = ControlInterface.getRearmCatapult();
        EventSource fireButton = ControlInterface.getFireButton();
        // ***** DRIVE JOYSTICK *****
        FloatInputPoll leftDriveAxis = Mixing.negate(joystick1.getAxisChannel(2));
        FloatInputPoll forwardDriveAxis = Mixing.negate(joystick1.getAxisChannel(3));
        FloatInputPoll rightDriveAxis = Mixing.negate(joystick1.getAxisChannel(5));
        EventSource shiftHighButton = joystick1.getButtonSource(1);
        EventSource shiftLowButton = joystick1.getButtonSource(3);
        // [[[[ AUTONOMOUS CODE ]]]]
        AutonomousController controller = new AutonomousController();
        controller.setup(this);
        controller.putDriveMotors(leftDrive1, leftDrive2, rightDrive1, rightDrive2);
        controller.putHotzone(isHotZone);
        controller.putUltrasonic(distance);
        controller.putArm(armSolenoid, collectorMotor);
        EventSource fireAutonomousTrigger = controller.getWhenToFire();
        // [[[[ DRIVE CODE ]]]]
        BooleanStatus shiftBoolean = DriveCode.createShifting(startedTeleop, duringTeleop, shiftSolenoid, shiftHighButton, shiftLowButton);
        DriveCode.createDrive(startedTeleop, duringTeleop, leftDrive1, leftDrive2, rightDrive1, rightDrive2, leftDriveAxis, rightDriveAxis, forwardDriveAxis, IS_COMPETITION_ROBOT, shiftBoolean);
        // [[[[ SHOOTER CODE ]]]]
        EventSource fireWhen = Mixing.combine(fireAutonomousTrigger, fireButton);
        EventLogger.log(fireWhen, LogLevel.FINE, "Fire now!");
        EventSource updateShooterWhen = Mixing.combine(duringTeleop, duringAutonomous);
        BooleanStatus canCollectorRun = new BooleanStatus();
        BooleanStatus winchEngaged=new BooleanStatus(winchSolenoid);
        BooleanInputPoll rearming = Shooter.createShooter(
                startedAutonomous, startedTeleop, updateShooterWhen, constantPeriodic,
                winchMotor,
                winchSolenoid,
                winchCurrent, ControlInterface.powerSlider(),
                catapultNotCocked, armUpDown, detensioning,
                rearmCatapult, fireWhen, canCollectorRun,
                winchEngaged
        );
        Shooter.createTuner(globalPeriodic, winchCurrent, rearmCatapult, catapultNotCocked);
        // [[[[ ARM CODE ]]]]
        Actuators.createArm(duringTeleop, armSolenoid, armUpDown, IS_COMPETITION_ROBOT ? Mixing.alwaysTrue:rearming);
        Actuators.createCollector(duringTeleop, collectorMotor, armFloatSolenoid, rollersIn, rollersOut, canCollectorRun);
        // [[[[ Phidget Display Code ]]]]
        ControlInterface.displayPressure(pressureSensor, globalPeriodic, pressureSwitch);
        ControlInterface.displayDistance(distance, globalPeriodic);
        ControlInterface.showRearming(globalPeriodic, rearming);
        ControlInterface.showFiring(globalPeriodic, winchEngaged);
        //MOTD.createMOTD();
    }
}
