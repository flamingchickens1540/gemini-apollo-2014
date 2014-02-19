package org.team1540.geminiapollo;

import ccre.cluck.CluckGlobals;
import ccre.cluck.CluckNode;
import ccre.cluck.CluckSubscriber;
import ccre.event.EventConsumer;
import ccre.phidget.PhidgetReader;
import java.util.Random;

// I moved this into its own module because the code is more organized this way,
// and I fixed it to not be constantly sending the message because it slows down
// the network.
// I cleaned up your code in a few other ways.
public class MOTD {

    public static void createMOTD() {
        final String[] messages = new String[]{
            "Welcome to pain!", "Are we human?", "Five Guys B&F's!",
            "My brother...", "My captain...", "My king.", "I will be back.",
            "Only twenty characters", "Blood", "Smoking kills.", "Peer",
            "Just say no!", "SPAAAAAAAAAAAAACE!!!", "$YOLO SWAGGINS$",
            "Don't drop the soap.", "This is SPARTA!!!", "Close the door...",
            "Get a room.", "SINUSOIDS", "Iron", "That's not a knife!", "Knoife",
            "Lana. Lana! LAAANNAA!", "David Whitson", "It's a trap!", "That's no moon!",
            "That's what she said", "My precious...", "Ha. Haha. Ha.",
            "I get that reference", "I'm always angry.", "I said... I said....",
            "Tactical Turtleneck", "Ah yes, 'Reapers.'", "OP: Fire-Cobra-Claw"};
        final String message = messages[new Random().nextInt(messages.length)];
        final EventConsumer updateDisplay = new EventConsumer() {
            public void eventFired() {
                PhidgetReader.phidgetLCD[0].println(message);
            }
        };
        updateDisplay.eventFired();
        CluckGlobals.node.addLink(new CluckSubscriber() {
            protected void receive(String source, byte[] data) {
            }

            protected void receiveBroadcast(String source, byte[] data) {
                if (data.length == 1 && data[0] == CluckNode.RMT_NOTIFY) {
                    updateDisplay.eventFired();
                }
            }
        }, "netmonitor");
    }

}
