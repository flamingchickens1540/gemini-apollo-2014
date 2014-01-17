/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import ccre.cluck.CluckGlobals;
import ccre.event.EventConsumer;
import ccre.event.EventSource;
import ccre.log.Logger;
import ccre.chan.*;
import ccre.log.LoggingTarget;
import java.io.OutputStream;

/**
 *
 * @author peachg
 */
class TestMode {
    private static boolean published=false;
    public static void start(EventSource when,Object[] tobepublished,String[] names) {
        if(published){
            Logger.warning("Tried to double log.");
            return;
        }
        for(int x=0;x<tobepublished.length;x++){
            publishWhatever(tobepublished[x],names[x]);
        }

    }
    //This is a horible method. Must be fixed. (Internal ccre fix?) -G
    private static void publishWhatever(Object o,String s){
        if(o instanceof BooleanStatus){
            CluckGlobals.node.publish(s, (BooleanStatus) o);
            return;
        }
        if(o instanceof BooleanInput){
            CluckGlobals.node.publish(s, (BooleanInput) o);
            return;
        }
        if(o instanceof BooleanOutput){
            CluckGlobals.node.publish(s, (BooleanOutput) o);
            return;
        }
        if(o instanceof BooleanInputProducer){
            CluckGlobals.node.publish(s, (BooleanInputProducer) o);
            return;
        }
        if(o instanceof BooleanStatus){
            CluckGlobals.node.publish(s, (BooleanStatus) o);
            return;
        }
        if(o instanceof FloatInput){
            CluckGlobals.node.publish(s, (FloatInput) o);
            return;
        }
        if(o instanceof FloatOutput){
            CluckGlobals.node.publish(s, (FloatOutput) o);
            return;
        }
        if(o instanceof FloatInputProducer){
            CluckGlobals.node.publish(s, (FloatInputProducer) o);
            return;
        }
        if(o instanceof EventConsumer){
            CluckGlobals.node.publish(s, (EventConsumer) o);
            return;
        }
        if(o instanceof EventSource){
            CluckGlobals.node.publish(s, (EventSource) o);
            return;
        }
        if(o instanceof LoggingTarget){
            CluckGlobals.node.publish(s, (LoggingTarget) o);
            return;
        }
        if(o instanceof OutputStream){
            CluckGlobals.node.publish(s, (OutputStream) o);
            return;
        }
        Logger.warning("could not be logged!");
        return;
    }
    
}
