package org.team1540.geminiapollo;

import ccre.event.EventConsumer;
import ccre.event.EventSource;
import ccre.phidget.PhidgetReader;
import java.io.PrintStream;

public class ErrorMessages {

    private static String activeMessage = null;
    private static int activePriority = -1;
    private static int timeRemaining = -1;

    public static void setupError(EventSource constant) {
        PrintStream line = PhidgetReader.phidgetLCD[0];
        String defaultStr = RobotMain.IS_COMPETITION_ROBOT ? "=)=>== APOLLO ==)=>=" : "[___] GEMIINII [___]";
        line.println("");
        constant.addListener(new EventConsumer() {
            public void eventFired() {
                if (activeMessage != null) {
                    timeRemaining -= 10;
                    if (timeRemaining <= 0) {
                        activeMessage = null;
                        activePriority = -1;
                        timeRemaining = -1;
                        
                    }
                }
                
            }
        });
    }

    public static void displayError(int priority, String message, int timeoutMillis) {
        errno = (errno + 1) % 10;
        .print(errno + " " + message + '\n');
    }
}
