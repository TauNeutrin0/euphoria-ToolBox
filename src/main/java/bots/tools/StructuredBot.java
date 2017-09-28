package bots.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import euphoria.Bot;
import euphoria.RoomConnection;
import euphoria.RoomNotConnectedException;
import euphoria.events.ConnectionEvent;

import euphoria.events.ConnectionEventListener;
import euphoria.events.MessageEvent;
import euphoria.events.PacketEvent;
import euphoria.events.PacketEventListener;
import euphoria.events.ReplyEventListener;
import euphoria.packets.StandardPacket;
import euphoria.packets.commands.Nick;
import euphoria.packets.commands.Send;


public abstract class StructuredBot extends Bot{
    //private EventListenerList listeners = new EventListenerList();
    //private Map<String, EventListenerList> roomListeners = new HashMap<String, EventListenerList>();
    //private Map<String,PausedEventListener> pauseListeners  = new HashMap<String,PausedEventListener>();
    //private Map<String, Boolean> roomCollapsed = new HashMap<String, Boolean>();
    private Map<String, RoomConnection> collapsedConnections = new HashMap<String, RoomConnection>();
    private ArrayList<String> structurePendingRooms = new ArrayList<String>();
    public StructuredBot parent = null;
    private ArrayList<StructuredBot> children = new ArrayList<StructuredBot>();
    private String nick;
    private Object nickLock = new Object();
    private final PacketEventListener PARENT_HOOK = new PacketEventListener() {
        @Override
        public void packetRecieved(PacketEvent evt) {
            StructuredBot.this.collapsedConnections.get(evt.getRoomConnection().getRoom()).handlePacket(evt.getPacket());
        }
        public void onSnapshotEvent(PacketEvent evt) {}
        public void onSendEvent(MessageEvent evt) {}
        public void onPartEvent(PacketEvent evt) {}
        public void onNickEvent(PacketEvent evt) {}
        public void onJoinEvent(PacketEvent evt) {}
        public void onHelloEvent(PacketEvent evt) {}
        public void onBounceEvent(PacketEvent evt) {}
    };
    private final ConnectionEventListener COLLAPSE_HANDLER = new ConnectionEventListener() {
        @Override
        public void onConnect(ConnectionEvent evt) {
            collapsedConnections.remove(evt.getRoomConnection().getRoom());
            evt.getRoomConnection().listeners.remove(ConnectionEventListener.class, this);
            try {
                StructuredBot.this.parent.getRoomConnection(evt.getRoomConnection().getRoom()).listeners.remove(PacketEventListener.class,PARENT_HOOK);
            } catch (RoomNotConnectedException e) {
                System.err.println("Parent of @"+nick+" not connected in &"+evt.getRoomConnection().getRoom()+".");
            }
        }
        @Override
        public void onDisconnect(ConnectionEvent evt) {
            collapsedConnections.put(evt.getRoomConnection().getRoom(),evt.getRoomConnection());
            try {
                StructuredBot.this.parent.getRoomConnection(evt.getRoomConnection().getRoom()).listeners.add(PacketEventListener.class,PARENT_HOOK);
            } catch (RoomNotConnectedException e) {
                System.err.println("Parent of @"+nick+" not connected in &"+evt.getRoomConnection().getRoom()+".");
            }
        }
        public void onConnectionError(ConnectionEvent evt) {}
    };
    
    public StructuredBot(String nick) {
        this.nick = nick;
    }
    
    public StructuredBot(StructuredBot parent, String nick) {
        this.parent = parent;
        this.nick = nick;
    }
    
    public void setCollapsed(String room, boolean isCollapsed) {
        if(parent!=null){
            if(!collapsedConnections.containsKey(room)&&isCollapsed){
                try {
                    getRoomConnection(room).listeners.add(ConnectionEventListener.class, COLLAPSE_HANDLER);
                    disconnectRoom(room);
                } catch (RoomNotConnectedException e) {}
            } else if(collapsedConnections.containsKey(room)&&!isCollapsed){
                startRoomConnection(collapsedConnections.get(room));
                //connectRoom(room);
            }
        }

    }
    
    public void connectStructureToRoom(final String room) {
        if(!isConnected(room)){
            if(parent==null||parent.isConnected(room)){
                connectRoom(room);            
            } else {
                structurePendingRooms.add(room);
                parent.connectStructureToRoom(room, new ConnectionEventListener() {
                    @Override
                    public void onConnect(ConnectionEvent evt) {
                        connectRoom(room);
                        structurePendingRooms.remove(room);
                    }
    
                    public void onDisconnect(ConnectionEvent evt) {}
    
                    public void onConnectionError(ConnectionEvent evt) {
                        structurePendingRooms.remove(room);
                    }
                    
                });
            }
        }

    }
    
    public void connectStructureToRoom(final String room, final ConnectionEventListener eL) {
        if(!isConnected(room)){
            if(parent==null||parent.isConnected(room)){
                connectRoom(room,eL);            
            } else {
                structurePendingRooms.add(room);
                parent.connectStructureToRoom(room, new ConnectionEventListener() {
                    @Override
                    public void onConnect(ConnectionEvent evt) {
                        connectRoom(room);
                        structurePendingRooms.remove(room);
                        eL.onConnect(evt);
                    }
    
                    public void onDisconnect(ConnectionEvent evt) {eL.onDisconnect(evt);}
    
                    public void onConnectionError(ConnectionEvent evt) {
                        eL.onConnectionError(evt);
                        structurePendingRooms.remove(room);}
                    
                });
            }
        }

    }
    
    public void disconnectStructureFromRoom(String room) {
        for(StructuredBot child : children) {
            if(child.isConnected(room)){
                child.disconnectStructureFromRoom(room);
            }
        }
        disconnectRoom(room);
    }
    
    public boolean isCollapsed(String room) {
        return collapsedConnections.containsKey(room);
    }
    
    public boolean isStructurePending(String room) {
        return structurePendingRooms.contains(room);
    }
    
    public void sendPacket(StandardPacket pckt, String room, ReplyEventListener l) {
        if(isConnected(room)||isCollapsed(room)){
            try{
                // Check if bot is collapsed in this room.
                if(collapsedConnections.containsKey(room)) {
                    // If it is collapsed, send packet to parent for handling.
                    final String currNick = this.collapsedConnections.get(room).getNick();
                    if(parent!=null){
                        parent.sendPacketAs(pckt, currNick, room, l);
                    } else {
                        System.err.println("@" + currNick + " collapsed in &"+room+" but has no parent. Cannot send packet.");
                    }
                } else {
                   // If it is not collapsed, send the packet.
                    getRoomConnection(room).sendPacket(pckt);
                }
            } catch(RoomNotConnectedException e){
                
            }
        } else {
            System.err.println("@" + nick + " not connected to &" + room + ". Cannot send packet.");
        }


    }
    
    public void sendPacketAs(final StandardPacket packet, String nick, String room, final ReplyEventListener listener) {
        synchronized(connectionLock){
            // Check if bot is collapsed in this room.
            if(isCollapsed(room)) {
                // If it is collapsed, send packet to parent for handling.
                if(parent!=null){
                    parent.sendPacketAs(packet, nick, room, listener);
                } else {
                    System.err.println("@" + nick + " collapsed in &"+room+" but has no parent. Cannot send packet.");
                }
            } else if(isConnected(room)) {
                
                try {
                    // If it is not collapsed
                    // Check if the required nick is already held by the bot.
                    final String currNick = this.getRoomConnection(room).getNick();
                    
                    if(nick.equals(currNick)){
                        // If so, send the packet.
                        getRoomConnection(room).sendPacket(packet);
                    } else {
                        // Otherwise, change the nick before sending packet.
                        synchronized(nickLock){ // Synchronize on nickLock to prevent interleaving on euphoria's nick.
                            try {
                                getRoomConnection(room).sendPacket(new Nick(nick).createPacket());     // Change nick
                                getRoomConnection(room).sendPacket(packet);                            // Send packet
                                getRoomConnection(room).sendPacket(new Nick(currNick).createPacket()); // Reset nick
                                
                                /* Unnecessary since TCP guarantees packets will be recieved in the right order.
                                // Change nick
                                getRoomConnection(room).sendPacket(new Nick(nick).createPacket(), new ReplyEventListener() {
                                    @Override
                                    public void onReplySuccess(PacketEvent evt) {
                                        // When nick change is acnowledged, send packet.
                                        evt.getRoomConnection().sendPacket(packet, new ReplyEventListener() {
                                            public void onReplySuccess(PacketEvent evt) {if(listener!=null)listener.onReplySuccess(evt);}
                                            public void onReplyFail(PacketEvent evt) {if(listener!=null)listener.onReplyFail(evt);}
                                            @Override
                                            public void onReplyEvent(PacketEvent evt) {
                                                // When packet is acnowledged, change nick back.
                                                try{
                                                    evt.getRoomConnection().sendPacket(new Nick(currNick).createPacket());
                                                } finally {if(listener!=null)listener.onReplyEvent(evt);}
                                            }
                                        });
                                    }
                                    
                                    @Override
                                    public void onReplyFail(PacketEvent evt) {
                                        System.err.println("Could not update nick.");
                                    }
                                    public void onReplyEvent(PacketEvent evt) {}
                                });
                                */
                            } catch (RoomNotConnectedException e) {
                                System.err.println("@" + nick + " not connected to &" + room + ". Cannot send packet.");
                            }
                        }
                    }
                } catch (RoomNotConnectedException e) {/* Should never happen */}
            } else {
                System.err.println("@" + nick + " not connected to &" + room + ". Cannot send packet.");
            }
        }


    }
    
    public void reply(MessageEvent evt, String message, ReplyEventListener l) {
        sendPacketAs(new Send(message, evt.getId()).createPacket(), evt.getRoomConnection().getNick(), evt.getRoomConnection().getRoom(), l);
    }
}
