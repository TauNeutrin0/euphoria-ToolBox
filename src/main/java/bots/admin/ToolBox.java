package bots.admin;

import bots.AnnouncerBot;
import bots.Child;
import bots.Parent;
import bots.euphoMail;
import bots.tools.ConnectMessageEventListener;
import bots.tools.StandardEventListener;
import bots.tools.StructuredBot;
import euphoria.FileIO;
import euphoria.events.PacketEventListener;

public class ToolBox extends StructuredBot {
    FileIO dataFile;
    
    public ToolBox() {
        super("ToolBox");
        dataFile = new FileIO("ToolBox_data");
        useCookies(dataFile);
        
        listeners.add(PacketEventListener.class,new StandardEventListener(this,"ToolBox","I'm a test bot made by @TauNeutrin0. Hi!"));
        
        ConnectMessageEventListener cMEL = new ConnectMessageEventListener("ToolBox",this,dataFile).connectAll();
        addConsoleListener(cMEL);
        listeners.add(PacketEventListener.class,cMEL);
        
    }
    
    public static void main(String[] args) {
        ToolBox tB = new ToolBox();
        new euphoMail(tB);
        new AnnouncerBot(tB);
        new Child(new Parent(tB));
    }
    
}
