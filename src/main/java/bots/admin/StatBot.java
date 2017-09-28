package bots.admin;

import java.util.HashMap;
import java.util.HashSet;

import euphoria.events.MessageEvent;
import euphoria.events.PacketEvent;
import euphoria.events.PacketEventListener;
import bots.tools.StructuredBot;

public class StatBot extends StructuredBot {
    private HashMap<String, HashSet<User>> roomUsers;
    
    
    public StatBot(StructuredBot parent) {
        super(parent,"StatBot");
        new PacketEventListener() {

            @Override
            public void onBounceEvent(PacketEvent evt) {}

            @Override
            public void onHelloEvent(PacketEvent evt) {}

            @Override
            public void onJoinEvent(PacketEvent evt) {}

            @Override
            public void onNickEvent(PacketEvent evt) {}

            @Override
            public void onPartEvent(PacketEvent evt) {}

            @Override
            public void onSendEvent(MessageEvent evt) {
              //Update user timestamp
            }

            @Override
            public void onSnapshotEvent(PacketEvent evt) {
              
            }

            public void packetRecieved(PacketEvent evt) {}
            
        };
    }
}
