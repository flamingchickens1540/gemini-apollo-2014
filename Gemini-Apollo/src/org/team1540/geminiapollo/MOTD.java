package org.team1540.geminiapollo;

import ccre.chan.BooleanOutput;
import ccre.cluck.CluckGlobals;
import ccre.cluck.CluckNode;
import ccre.cluck.CluckSubscriber;
import ccre.ctrl.Mixing;
import ccre.event.EventConsumer;
import ccre.log.Logger;
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
            "Get a room.", "SINUSOIDS", "Iron", "That's not a knife!", "Knoife"};
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
        PhidgetReader.attached.addTarget(new BooleanOutput() {
            public void writeValue(boolean value) {
                Logger.fine("New attached value: " + value);
            }
        });
        Mixing.whenBooleanBecomes(PhidgetReader.attached, true).addListener(updateDisplay);
    }

}
