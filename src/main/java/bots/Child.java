package bots;

import bots.tools.ConnectMessageEventListener;
import bots.tools.StandardEventListener;
import bots.tools.StructuredBot;
import euphoria.FileIO;
import euphoria.events.PacketEventListener;

public class Child extends StructuredBot {
    FileIO dataFile;
    
    public Child(StructuredBot parent) {
        super(parent,"Child");
        
        dataFile = new FileIO("child_data");
        useCookies(dataFile);
        
        listeners.add(PacketEventListener.class,new StandardEventListener(this,"Child","I'm a test bot made by @TauNeutrin0. Hi!"));
        
        ConnectMessageEventListener cMEL = new ConnectMessageEventListener("Child",this,dataFile).connectAll();
        addConsoleListener(cMEL);
        listeners.add(PacketEventListener.class,cMEL);
    }
}
