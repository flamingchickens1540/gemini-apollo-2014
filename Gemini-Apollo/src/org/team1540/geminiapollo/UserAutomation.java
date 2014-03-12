package org.team1540.geminiapollo;

import ccre.chan.*;
import ccre.ctrl.ExpirationTimer;
import ccre.event.*;

public class UserAutomation {

    public static BooleanInputPoll setupAuto(EventSource doResetBall, BooleanOutput runArm) {
        ExpirationTimer timer = new ExpirationTimer();
        timer.startWhen(doResetBall);
        timer.stopWhen(timer.schedule(1200));
        timer.scheduleBooleanPeriod(1, 600, runArm, true);
        BooleanStatus overrideCollector = new BooleanStatus();
        timer.scheduleBooleanPeriod(1, 1200, overrideCollector, true);
        return overrideCollector;
    }
}
