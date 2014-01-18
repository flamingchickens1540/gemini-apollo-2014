package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.chan.BooleanOutput;
import ccre.chan.FloatInputPoll;
import ccre.chan.FloatOutput;
import ccre.event.EventSource;
import ccre.igneous.SimpleCore;

public class RobotMain extends SimpleCore {

    protected void createSimpleControl() {
        // ***** MOTORS *****
        // TODO: Better selection of ramping settings
        FloatOutput leftDrive1 = makeTalonMotor(1, MOTOR_FORWARD, 0.1f);
        FloatOutput leftDrive2 = makeTalonMotor(2, MOTOR_FORWARD, 0.1f);
        FloatOutput rightDrive1 = makeTalonMotor(3, MOTOR_FORWARD, 0.1f);
        FloatOutput rightDrive2 = makeTalonMotor(4, MOTOR_FORWARD, 0.1f);
        FloatOutput winchMotor = makeTalonMotor(5, MOTOR_FORWARD, 0.1f);
        FloatOutput collectorMotor = makeTalonMotor(6, MOTOR_FORWARD, 0.1f);

        // ***** SOLENOIDS *****
        BooleanOutput shiftSolenoid = makeSolenoid(1);
        BooleanOutput armSolenoid = makeSolenoid(2);
        BooleanOutput winchReleaseSolenoid = makeSolenoid(3);
        BooleanOutput winchEngageSolenoid = makeSolenoid(4);

        // ***** ANALOG INPUTS *****
        // TODO: Better selection of average bits
        FloatInputPoll winchCurrent = makeAnalogInput(1, 8);
        FloatInputPoll pressureSensor = makeAnalogInput(2, 8);

        // ***** DIGITAL INPUTS *****
        BooleanInputPoll catapultCocked = makeDigitalInput(2);

        // ***** COMPRESSOR *****
        useCompressor(1, 1);

        // ***** CONTROL INTERFACE *****
        // TODO: Check if these should be Producers.
        BooleanInputPoll armUpDown = ControlInterface.getArmUpDown();
        BooleanInputPoll rollersOnOff = ControlInterface.getRollersOnOff();
        EventSource rearmCatapult = ControlInterface.getRearmCatapult();
        EventSource fireButton = ControlInterface.getFireButton();
        
        FloatInputPoll pullbackMod = ControlInterface.getPullbackSlider();

        // ***** DRIVE JOYSTICK *****
        FloatInputPoll leftDriveAxis = joystick1.getAxisChannel(2);
        FloatInputPoll forwardDriveAxis = joystick1.getAxisChannel(3);
        FloatInputPoll rightDriveAxis = joystick1.getAxisChannel(5);

        EventSource shiftHighButton = joystick1.getButtonSource(1);
        EventSource shiftLowButton = joystick1.getButtonSource(3);

        // [[[[ DRIVE CODE ]]]]
        DriveCode.createDrive(startedTeleop, duringTeleop, leftDrive1, leftDrive2, rightDrive1, rightDrive2, leftDriveAxis, rightDriveAxis, forwardDriveAxis);
        DriveCode.createShifting(startedTeleop, duringTeleop, shiftSolenoid, shiftHighButton, shiftLowButton);

        // [[[[ ARM CODE ]]]]
        Actuators.createCollector(startedTeleop, duringTeleop, collectorMotor, rollersOnOff);
        Actuators.createArm(startedTeleop, duringTeleop, armSolenoid, armUpDown);

        // [[[[ SHOOTER CODE ]]]]
        Shooter.createShooter(startedTeleop, duringTeleop, winchMotor, winchEngageSolenoid, winchReleaseSolenoid, winchCurrent, catapultCocked, rearmCatapult, fireButton, pullbackMod);
        // TODO: VisionTracking calls not added yet.
        // TODO: Autonomous calls not added yet.
        // TODO: TestMode calls not added yet.

        // TODO: Display current pressure.
    }
}
