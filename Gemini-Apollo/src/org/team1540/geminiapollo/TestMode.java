package org.team1540.geminiapollo;

import ccre.cluck.CluckGlobals;
import ccre.event.EventConsumer;
import ccre.event.EventSource;
import ccre.chan.*;

public class TestMode {
    public BooleanInputPoll inTest;
    public TestMode(BooleanInputPoll inTest) {
        this.inTest=inTest;
    }
    public void testPublish(String s,BooleanStatus b){
        CluckGlobals.node.publish(s+".input",(BooleanInput) b);
        testPublish(s+".output",(BooleanOutput) b);
    }
    public void testPublish(String s,BooleanInput b){
        CluckGlobals.node.publish(s,(BooleanInput) b);
    }
    public void testPublish(String s,final BooleanOutput b){
        CluckGlobals.node.publish(s, new BooleanOutput(){
            public void writeValue(boolean bln) {
                if(inTest.readValue()){
                    b.writeValue(bln);
                }
            }
        });
    }
    public void testPublish(String s,FloatStatus b){
        CluckGlobals.node.publish(s+".input",(FloatInput) b);
        testPublish(s+".output",(FloatOutput) b);
    }
    public void testPublish(String s,FloatInput b){
        CluckGlobals.node.publish(s,(FloatInput) b);
    }
    public void testPublish(String s,final FloatOutput b){
        CluckGlobals.node.publish(s, new FloatOutput(){
            public void writeValue(float bln) {
                if(inTest.readValue()){
                    b.writeValue(bln);
                }
            }
        });
    }
    public void testPublish(String s,BooleanInputProducer b){
        CluckGlobals.node.publish(s, b);
    }
    public void testPublish(String s,FloatInputProducer b){
        CluckGlobals.node.publish(s, b);
    }
    public void testPublish(String s,final EventConsumer o){
        CluckGlobals.node.publish(s,new EventConsumer(){
            public void eventFired() {
                if(inTest.readValue()){
                    o.eventFired();
                }
            }
        });
    }
    public void testPublish(String s,final EventSource o){
        CluckGlobals.node.publish(s,o);
    }

}
