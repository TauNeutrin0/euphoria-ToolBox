package bots;

import bots.tools.ConnectMessageEventListener;
import bots.tools.StandardEventListener;
import bots.tools.StructuredBot;
import euphoria.FileIO;
import euphoria.events.PacketEventListener;

public class Parent extends StructuredBot {
    FileIO dataFile;
    
    public Parent(StructuredBot parent) {
        super(parent,"Parent");
        
        dataFile = new FileIO("parent_data");
        useCookies(dataFile);
        
        listeners.add(PacketEventListener.class,new StandardEventListener(this,"Parent","I'm a test bot made by @TauNeutrin0. Hi!"));
        
        ConnectMessageEventListener cMEL = new ConnectMessageEventListener("Parent",this,dataFile).connectAll();
        addConsoleListener(cMEL);
        listeners.add(PacketEventListener.class,cMEL);
    }
}
