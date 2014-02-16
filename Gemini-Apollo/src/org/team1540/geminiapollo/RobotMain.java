package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.cluck.tcp.CluckTCPServer;
import ccre.ctrl.Mixing;
import ccre.event.*;
import ccre.igneous.SimpleCore;
import ccre.log.*;

public class RobotMain extends SimpleCore {

    public static boolean IS_COMPETITION_ROBOT = false;

    protected void createSimpleControl() {
        // ***** CLUCK *****
        new CluckTCPServer(CluckGlobals.node, 443).start();
        new CluckTCPServer(CluckGlobals.node, 1130).start();
        new CluckTCPServer(CluckGlobals.node, 1140).start();
        new CluckTCPServer(CluckGlobals.node, 1180).start();
        new CluckTCPServer(CluckGlobals.node, 1540).start();
        new CluckTCPServer(CluckGlobals.node, 1735).start();
        TestMode test = new TestMode(getIsTest());
        // ***** MOTORS *****
        // TODO: Better selection of ramping settings
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
        BooleanOutput winchSolenoid = test.testPublish("sol-winch-3", makeSolenoid(3));
        BooleanOutput rachetLoopRelease = test.testPublish("sol-rachet-5", makeSolenoid(5));
        BooleanOutput armFloatSolenoid = test.testPublish("sol-float-6", makeSolenoid(6));

        // ***** ANALOG INPUTS *****
        // TODO: Better selection of average bits
        FloatInputPoll winchCurrent = makeAnalogInput(1, 8);
        FloatInputPoll pressureSensor = makeAnalogInput(2, 8);
        FloatInputPoll ultrasonicSensor = makeAnalogInput(3, 8);
        CluckGlobals.node.publish("Winch Current", Mixing.createDispatch(pressureSensor, globalPeriodic));
        CluckGlobals.node.publish("Pressure Sensor", Mixing.createDispatch(pressureSensor, globalPeriodic));
        CluckGlobals.node.publish("Ultrasonic Sensor", Mixing.createDispatch(pressureSensor, globalPeriodic));

        // ***** DIGITAL INPUTS *****
        BooleanInputPoll catapultNotCocked = makeDigitalInput(2);

        // ***** VISION TRACKING *****
        VisionTracking.setup(startedAutonomous);
        BooleanInputPoll isHotZone = VisionTracking.isHotZone();
        // ***** COMPRESSOR *****
        useCompressor(1, 1);

        // ***** CONTROL INTERFACE *****
        BooleanInputPoll armUpDown = ControlInterface.getArmUpDown();
        BooleanInputPoll rollersIn = ControlInterface.rollerIn();
        BooleanInputPoll rollersOut = ControlInterface.rollerOut();
        BooleanInputPoll detensioning=ControlInterface.detensioning();
        EventSource rearmCatapult = ControlInterface.getRearmCatapult();
        EventSource fireButton = ControlInterface.getFireButton();
        ControlInterface.displayPressure(pressureSensor, globalPeriodic);

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
        controller.putUltrasonic(ultrasonicSensor);
        EventSource fireAutonomousTrigger = controller.getWhenToFire();

        // [[[[ DRIVE CODE ]]]]
        BooleanStatus shiftBoolean = DriveCode.createShifting(startedTeleop, duringTeleop, shiftSolenoid, shiftHighButton, shiftLowButton);
        DriveCode.createDrive(startedTeleop, duringTeleop, leftDrive1, leftDrive2, rightDrive1, rightDrive2, leftDriveAxis, rightDriveAxis, forwardDriveAxis, IS_COMPETITION_ROBOT, shiftBoolean);

        // [[[[ SHOOTER CODE ]]]]
        EventSource fireWhen=combine(fireAutonomousTrigger,fireButton);
        EventLogger.log(fireWhen, LogLevel.FINE, "Fire now!");
        EventSource updateShooterWhen=combine(duringTeleop,duringAutonomous);
        BooleanInputPoll canArmMove = Shooter.createShooter(
                startedAutonomous,startedTeleop,updateShooterWhen,
                winchMotor,
                winchSolenoid,rachetLoopRelease,
                winchCurrent,
                catapultNotCocked,armUpDown,detensioning,
                rearmCatapult,fireWhen
        );
        // [[[[ ARM CODE ]]]]
        Logger.info("Actuators get startedTeleop irrelevently!");
        Actuators.createCollector(startedTeleop, duringTeleop, collectorMotor, armFloatSolenoid, rollersIn,rollersOut);
        Actuators.createArm(startedTeleop, duringTeleop, armSolenoid, armUpDown, canArmMove);

        // [[[[ MOTD CODE ]]]]
        MOTD.createMOTD();
    }
    public EventSource combine(final EventSource a,final EventSource b){
        return new EventSource(){
            public boolean addListener(EventConsumer ec) {
                return a.addListener(ec) && b.addListener(ec);
            }
            public void removeListener(EventConsumer ec) throws IllegalStateException {
                a.removeListener(ec);
                b.removeListener(ec);
            }
        };
    }
}
