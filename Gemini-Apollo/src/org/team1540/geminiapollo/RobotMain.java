package org.team1540.geminiapollo;

import ccre.chan.BooleanInput;
import ccre.chan.BooleanInputPoll;
import ccre.chan.BooleanOutput;
import ccre.chan.FloatInputPoll;
import ccre.chan.FloatOutput;
import ccre.cluck.CluckGlobals;
import ccre.cluck.tcp.CluckTCPServer;
import ccre.ctrl.Mixing;
import ccre.event.Event;
import ccre.event.EventConsumer;
import ccre.event.EventLogger;
import ccre.event.EventSource;
import ccre.igneous.SimpleCore;
import ccre.log.LogLevel;
import ccre.log.Logger;
import ccre.phidget.PhidgetReader;
import ccre.saver.StorageProvider;
import ccre.saver.StorageSegment;
import java.util.Random;

public class RobotMain extends SimpleCore {

    protected void createSimpleControl() {
        new CluckTCPServer(CluckGlobals.node, 443).start();
        new CluckTCPServer(CluckGlobals.node, 1130).start();
        new CluckTCPServer(CluckGlobals.node, 1140).start();
        new CluckTCPServer(CluckGlobals.node, 1180).start();
        new CluckTCPServer(CluckGlobals.node, 1540).start();
        new CluckTCPServer(CluckGlobals.node, 1735).start();
        TestMode test = new TestMode(getIsTest());
        // ***** MOTORS *****
        // TODO: Better selection of ramping settings
        FloatOutput leftDrive1 = makeTalonMotor(1, MOTOR_REVERSE, 0.1f);
        FloatOutput leftDrive2 = makeTalonMotor(2, MOTOR_REVERSE, 0.1f);
        FloatOutput rightDrive1 = makeTalonMotor(3, MOTOR_FORWARD, 0.1f);
        FloatOutput rightDrive2 = makeTalonMotor(4, MOTOR_FORWARD, 0.1f);
        FloatOutput winchMotor = makeTalonMotor(5, MOTOR_FORWARD, 0.1f);
        FloatOutput collectorMotor = makeTalonMotor(6, MOTOR_FORWARD, 0.1f);

        // ***** SOLENOIDS *****
        BooleanOutput shiftSolenoid = makeSolenoid(1);
        BooleanOutput armSolenoid = makeSolenoid(2);
        BooleanOutput winchSolenoid = makeSolenoid(3);
        BooleanOutput rachetLoopRelease = makeSolenoid(5);
        BooleanOutput armFloatSolenoid = makeSolenoid(6);

        // ***** ANALOG INPUTS *****
        // TODO: Better selection of average bits
        FloatInputPoll winchCurrent = makeAnalogInput(1, 8);
        FloatInputPoll pressureSensor = makeAnalogInput(2, 8);
        CluckGlobals.node.publish("analog-sense", Mixing.createDispatch(pressureSensor, globalPeriodic));

        // ***** DIGITAL INPUTS *****
        BooleanInputPoll catapultCocked = makeDigitalInput(2);

        // ***** VISION TRACKING *****
        VisionTracking.setup(startedAutonomous);
        BooleanInputPoll isHotZone = VisionTracking.isHotZone();
        // ***** COMPRESSOR *****
        useCompressor(1, 1);

        // ***** CONTROL INTERFACE *****
        // TODO: Check if these should be Producers.
        BooleanInputPoll armUpDown = ControlInterface.getArmUpDown();
        BooleanInputPoll rollersOnOff = ControlInterface.getRollersOnOff();
        EventSource rearmCatapult = ControlInterface.getRearmCatapult();
        EventSource fireButton = ControlInterface.getFireButton();
        ControlInterface.displayPressure(pressureSensor, globalPeriodic);

        // ***** DRIVE JOYSTICK *****
        FloatInputPoll leftDriveAxis = joystick1.getAxisChannel(2);
        FloatInputPoll forwardDriveAxis = joystick1.getAxisChannel(3);
        FloatInputPoll rightDriveAxis = joystick1.getAxisChannel(5);

        EventSource shiftHighButton = joystick1.getButtonSource(1);
        EventSource shiftLowButton = joystick1.getButtonSource(3);

        // [[[[ AUTONOMOUS CODE ]]]]
        AutonomousController controller = new AutonomousController();
        controller.setup(this);
        controller.putDriveMotors(leftDrive1, leftDrive2, rightDrive1, rightDrive2);
        controller.putHotzone(isHotZone);
        EventSource fireAutonomousTrigger = controller.getWhenToFire();

        // [[[[ DRIVE CODE ]]]]
        DriveCode.createDrive(startedTeleop, duringTeleop, leftDrive1, leftDrive2, rightDrive1, rightDrive2, leftDriveAxis, rightDriveAxis, forwardDriveAxis);
        DriveCode.createShifting(startedTeleop, duringTeleop, shiftSolenoid, shiftHighButton, shiftLowButton);
        // Possible other way to control:
        //new DriveCode().setDriveMotors(leftDrive1, leftDrive2, rightDrive1, rightDrive2).setControlAxes(leftDriveAxis, rightDriveAxis, forwardDriveAxis).run(startedTeleop, duringTeleop);

        // [[[[ ARM CODE ]]]]
        Logger.info("Actuators get startedTeleop irrelevently!");
        Actuators.createCollector(startedTeleop, duringTeleop, collectorMotor, armFloatSolenoid, rollersOnOff);
        Actuators.createArm(startedTeleop, duringTeleop, armSolenoid, armUpDown);

        // [[[[ SHOOTER CODE ]]]]
        Event fireWhen = new Event();
        fireButton.addListener(fireWhen);
        fireAutonomousTrigger.addListener(fireWhen);
        EventLogger.log(fireWhen, LogLevel.FINE, "Fire now!");
        Event updateShooterWhen = new Event();
        duringTeleop.addListener(updateShooterWhen);
        duringAutonomous.addListener(updateShooterWhen);
        Shooter.createShooter(startedAutonomous, startedTeleop, updateShooterWhen, winchMotor, winchSolenoid, winchCurrent, catapultCocked, Mixing.filterEvent(getIsDisabled(), false, rearmCatapult), Mixing.filterEvent(getIsDisabled(), false, fireButton), armUpDown, rachetLoopRelease);

        // [[[[ MOTD CODE ]]]]
        MOTD.createMOTD();
    }
}
