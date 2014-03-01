package org.team1540.geminiapollo;

import ccre.cluck.CluckGlobals;
import ccre.event.*;
import ccre.chan.*;

public class TestMode {

    public final BooleanInputPoll inTest;

    public TestMode(BooleanInputPoll inTest) {
        this.inTest = inTest;
    }

    private static String testify(String name) {
        return "test_" + name;
    }

    public BooleanStatus testPublish(String s, BooleanStatus b) {
        s = testify(s);
        CluckGlobals.node.publish(s + ".input", (BooleanInput) b);
        testPublish(s + ".output", (BooleanOutput) b);
        return b;
    }

    public BooleanInput testPublish(String s, BooleanInput b) {
        CluckGlobals.node.publish(testify(s), (BooleanInput) b);
        return b;
    }

    public BooleanOutput testPublish(String s, final BooleanOutput b) {
        CluckGlobals.node.publish(testify(s), new BooleanOutput() {
            public void writeValue(boolean bln) {
                if (inTest.readValue()) {
                    b.writeValue(bln);
                }
            }
        });
        return b;
    }

    public FloatStatus testPublish(String s, FloatStatus b) {
        s = testify(s);
        CluckGlobals.node.publish(s + ".input", (FloatInput) b);
        testPublish(s + ".output", (FloatOutput) b);
        return b;
    }

    public FloatInput testPublish(String s, FloatInput b) {
        CluckGlobals.node.publish(testify(s), (FloatInput) b);
        return b;
    }

    public FloatOutput testPublish(String s, final FloatOutput b) {
        CluckGlobals.node.publish(testify(s), new FloatOutput() {
            public void writeValue(float bln) {
                if (inTest.readValue()) {
                    b.writeValue(bln);
                }
            }
        });
        return b;
    }

    public BooleanInputProducer testPublish(String s, BooleanInputProducer b) {
        CluckGlobals.node.publish(testify(s), b);
        return b;
    }

    public FloatInputProducer testPublish(String s, FloatInputProducer b) {
        CluckGlobals.node.publish(testify(s), b);
        return b;
    }

    public EventConsumer testPublish(String s, final EventConsumer o) {
        CluckGlobals.node.publish(testify(s), new EventConsumer() {
            public void eventFired() {
                if (inTest.readValue()) {
                    o.eventFired();
                }
            }
        });
        return o;
    }

    public EventSource testPublish(String s, final EventSource o) {
        CluckGlobals.node.publish(testify(s), o);
        return o;
    }

}
