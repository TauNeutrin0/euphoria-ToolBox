package bots;

import bots.tools.ConnectMessageEventListener;
import bots.tools.StandardEventListener;
import bots.tools.StructuredBot;
import euphoria.FileIO;
import euphoria.events.PacketEventListener;

public class MathBot extends StructuredBot {
    FileIO dataFile;

    public MathBot(StructuredBot parent) {
        super(parent, "MathBot");
        
        dataFile = new FileIO("MathBot_data");
        useCookies(dataFile);
        
        listeners.add(PacketEventListener.class,new StandardEventListener(this,"MathBot","I'm a test bot made by @TauNeutrin0. Hi!"));
        
        ConnectMessageEventListener cMEL = new ConnectMessageEventListener("MathBot",this,dataFile).connectAll();
        addConsoleListener(cMEL);
        listeners.add(PacketEventListener.class,cMEL);
        
    }
    
}
