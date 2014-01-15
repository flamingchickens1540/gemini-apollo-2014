package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.chan.BooleanOutput;
import ccre.chan.FloatInputPoll;
import ccre.chan.FloatOutput;
import ccre.event.EventSource;
import ccre.log.Logger;

public class Shooter {

    public static void createShooter(EventSource begin, EventSource during, FloatOutput winchMotor, BooleanOutput winchReleaseSolenoid, FloatInputPoll winchCurrent, BooleanInputPoll catapultCocked, BooleanInputPoll rearmCatapult, BooleanInputPoll fireButton) {
        Logger.warning("Shooter TODO");
    }
}
