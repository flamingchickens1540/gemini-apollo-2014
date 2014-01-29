package org.team1540.geminiapollo;

import ccre.chan.BooleanInputPoll;
import ccre.cluck.CluckGlobals;
import ccre.ctrl.ExpirationTimer;
import ccre.event.EventLogger;
import ccre.event.EventSource;
import ccre.log.LogLevel;

public class VisionTracking {

    public static String prefix = "periscope/";

    public static void setup(EventSource startAuto) {
        // Start vision tracking at the start of autonomous.
        startAuto.addListener(CluckGlobals.node.subscribeEC(prefix + "enable-vt-autonomous"));

        // Require an acknowledgement that the vision tracking is running.
        EventSource ack = CluckGlobals.node.subscribeES(prefix + "ack-vt-autonomous");
        EventLogger.log(ack, LogLevel.INFO, "Got ack!");
        ExpirationTimer checker = new ExpirationTimer();
        checker.startWhen(startAuto);
        checker.stopWhen(ack);
        checker.schedule(500, new EventLogger(LogLevel.WARNING, "No acknowledgement of vision tracking in 500 milliseconds!"));
        checker.schedule(510, checker.getStopEvent());
    }

    public static BooleanInputPoll isHotZone() {
        return CluckGlobals.node.subscribeBIP(prefix + "is-vt-autonomous", false);
    }
}
