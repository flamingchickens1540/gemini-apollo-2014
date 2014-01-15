package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.chan.BooleanOutput;
import ccre.chan.FloatInputPoll;
import ccre.chan.FloatOutput;
import ccre.igneous.SimpleCore;

public class RobotMain extends SimpleCore {

    public static final boolean IS_COMPETITION_ROBOT = false;

    protected void createSimpleControl() {
        
        // *** Inputs and outputs ***
        
        // TODO: Better selection of ramping settings
        FloatOutput leftDrive1 = makeTalonMotor(1, MOTOR_FORWARD, 0.1f);
        FloatOutput leftDrive2 = makeTalonMotor(2, MOTOR_FORWARD, 0.1f);
        FloatOutput rightDrive1 = makeTalonMotor(3, MOTOR_FORWARD, 0.1f);
        FloatOutput rightDrive2 = makeTalonMotor(4, MOTOR_FORWARD, 0.1f);
        FloatOutput winchMotor = makeTalonMotor(5, MOTOR_FORWARD, 0.1f);
        FloatOutput collectorMotor = makeTalonMotor(6, MOTOR_FORWARD, 0.1f);

        BooleanOutput shiftSolenoid = makeSolenoid(1);
        BooleanOutput armSolenoid = makeSolenoid(2);
        BooleanOutput winchReleaseSolenoid = makeSolenoid(3);

        // TODO: Better selection of average bits
        FloatInputPoll winchCurrent = makeAnalogInput(1, 8);
        FloatInputPoll pressureSensor = makeAnalogInput(2, 8);

        BooleanInputPoll catapultCocked = makeDigitalInput(2);

        useCompressor(1, 1);

        // TODO: Check if these should be Producers.
        BooleanInputPoll armUpDown = ControlInterface.getArmUpDown();
        BooleanInputPoll rollersOnOff = ControlInterface.getRollersOnOff();
        BooleanInputPoll rearmCatapult = ControlInterface.getRearmCatapult();
        BooleanInputPoll fireButton = ControlInterface.getFireButton();

        FloatInputPoll leftDriveAxis = joystick1.getAxisChannel(2);
        FloatInputPoll forwardDriveAxis = joystick1.getAxisChannel(3);
        FloatInputPoll rightDriveAxis = joystick1.getAxisChannel(5);

        BooleanInputPoll shiftHighButton = joystick1.getButtonChannel(1);
        BooleanInputPoll shiftLowButton = joystick1.getButtonChannel(3);

        // *** Drive code ***
        
        DriveCode.createDrive(startedTeleop, duringTeleop, leftDrive1, leftDrive2, rightDrive1, rightDrive2, leftDriveAxis, rightDriveAxis, forwardDriveAxis);
        DriveCode.createShifting(startedTeleop, duringTeleop, shiftSolenoid, shiftHighButton, shiftLowButton);

        Actuators.createCollector(startedTeleop, duringTeleop, collectorMotor, rollersOnOff);
        Actuators.createArm(startedTeleop, duringTeleop, armSolenoid, armUpDown);

        Shooter.createShooter(startedTeleop, duringTeleop, winchMotor, winchReleaseSolenoid, winchCurrent, catapultCocked, rearmCatapult, fireButton);
        // TODO: VisionTracking calls not added yet.
        // TODO: Autonomous calls not added yet.
        // TODO: TestMode calls not added yet.

        // TODO: Display current pressure.
    }
}
