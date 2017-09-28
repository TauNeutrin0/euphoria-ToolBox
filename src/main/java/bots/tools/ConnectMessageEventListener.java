package bots.tools;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import euphoria.FileIO;
import euphoria.RoomConnection;
import euphoria.RoomNotConnectedException;
import euphoria.events.ConnectionEvent;
import euphoria.events.ConnectionEventListener;
import euphoria.events.ConsoleEventListener;
import euphoria.events.MessageEvent;
import euphoria.events.PacketEvent;
import euphoria.events.PacketEventListener;
import euphoria.events.ReplyEventListener;

public class ConnectMessageEventListener implements PacketEventListener, ConsoleEventListener {
  StructuredBot bot;
  String nick;
  boolean hasDataFile = false;
  FileIO dataFile;
  
  public ConnectMessageEventListener(String nick, StructuredBot bot) {
    this.bot=bot;
    this.nick=nick;
  }
  
  public ConnectMessageEventListener(String nick, StructuredBot bot, FileIO dataFile) throws JsonParseException {
    this.bot=bot;
    this.nick=nick;
    this.dataFile = dataFile;
    synchronized(dataFile){
      JsonObject data = dataFile.getJson();
      if(data.has("rooms")){
        if(!data.get("rooms").isJsonArray()) {
          throw new JsonParseException("Invalid 'room' member found.");
        }
      } else {
        JsonArray arrayObject = new JsonArray();
        arrayObject.add("bots");
        data.add("rooms", arrayObject);
        try {
          dataFile.setJson(data);
        } catch (IOException e) {
          throw new JsonParseException("Could not create 'room' member.");
        }
      }
      if(dataFile.getJson().has("room-passwords")){
        if(!dataFile.getJson().get("room-passwords").isJsonObject()) {
          throw new JsonParseException("Invalid 'room-passwords' member found.");
        }
      } else {
        data.add("room-passwords", new JsonObject());
        try {
          dataFile.setJson(data);
        } catch (IOException e) {
          throw new JsonParseException("Could not create 'room-passwords' member.");
        }
      }
    }
    hasDataFile = true;
  }
  public ConnectMessageEventListener connectAll() {
    synchronized(dataFile){
      final JsonObject data = dataFile.getJson();
      for(int i=0;i<data.getAsJsonArray("rooms").size();i++){
        final String room = data.getAsJsonArray("rooms").get(i).getAsString();
        synchronized(bot.connectionLock){
            if(!(bot.isConnected(room)||bot.isPending(room)||bot.isStructurePending(room)||bot.isCollapsed(room))){
                if(bot.parent==null){
                  connectRoomInit(data,room);
                } else if(bot.parent.isConnected(room)){
                  connectRoomInit(data,room);
                } else if(bot.parent.isPending(room)) {
                  try {
                    bot.parent.getAnyRoomConnection(room).listeners.add(ConnectionEventListener.class,  new ConnectionEventListener() {
                            @Override
                            public void onConnect(ConnectionEvent evt) {
                              connectRoomInit(data,room);
                            }
                            public void onDisconnect(ConnectionEvent evt) {}
                            public void onConnectionError(ConnectionEvent evt) {}
                        });
                    } catch (RoomNotConnectedException e) {/* Should never happen */}
                } else {
                    bot.parent.connectStructureToRoom(room, new ConnectionEventListener() {
                        @Override
                        public void onConnect(ConnectionEvent evt) {
                          connectRoomInit(data,room);
                        }
                        public void onDisconnect(ConnectionEvent evt) {}
                        public void onConnectionError(ConnectionEvent evt) {}
                    });
                }
            }
        }

      }
    }
    return this;
  }

  @Override
  public void onSendEvent(MessageEvent evt) {
    if(evt.getMessage().matches("^!sendbot @"+nick+" &[A-Za-z0-9]+$")) {
      Pattern r = Pattern.compile("^!sendbot @"+nick+" &([A-Za-z0-9]+)$");
      final Matcher m = r.matcher(evt.getMessage());
      if (m.find()) {
        if(bot.isConnected(m.group(1))||bot.isPending(m.group(1))||bot.isStructurePending(m.group(1))||bot.isCollapsed(m.group(1))){
          boolean isBounced = false;
          try {
            isBounced = bot.getRoomConnection(m.group(1)).isBounced();
          } catch (RoomNotConnectedException e1) {
            e1.printStackTrace();
          }
          if(isBounced){
            bot.reply(evt,"&"+m.group(1)+" is private.",null);
            evt.getRoomConnection().closeConnection("Room is private.");
          } else if(hasDataFile) {
            boolean isStored = false;
            for(int i=0;i<dataFile.getJson().getAsJsonArray("rooms").size();i++){
              if(dataFile.getJson().getAsJsonArray("rooms").get(i).getAsString().equals(m.group(1))&&!m.group(1).equals("bots")){
                isStored = true;
              }
            }
            if(!isStored) {
              synchronized(dataFile){
                JsonObject data = dataFile.getJson();
                data.getAsJsonArray("rooms").add(m.group(1));
                try {
                  dataFile.setJson(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
              }
              bot.reply(evt,"/me has been added to &"+m.group(1),null);
            } else {
              bot.reply(evt,"/me is already in &"+m.group(1),null);
            }
          }
        } else {
            final MessageEvent event = evt;
            if(bot.parent==null){
                connectRoomEuphoria(m.group(1),event);
            } else if(bot.parent.isConnected(m.group(1))){
                connectRoomEuphoria(m.group(1),event);
            } else if(bot.parent.isPending(m.group(1))) {
                try {
                    bot.parent.getAnyRoomConnection(m.group(1)).listeners.add(ConnectionEventListener.class,  new ConnectionEventListener() {
                            @Override
                            public void onConnect(ConnectionEvent evt) {
                              connectRoomEuphoria(m.group(1),event);
                            }
                            public void onDisconnect(ConnectionEvent evt) {}
                            public void onConnectionError(ConnectionEvent evt) {}
                        });
                } catch (RoomNotConnectedException e) {/* Should never happen */}
            } else {
                bot.parent.connectStructureToRoom(m.group(1), new ConnectionEventListener() {
                    @Override
                    public void onConnect(ConnectionEvent evt) {
                        connectRoomEuphoria(m.group(1),event);
                    }
                    public void onDisconnect(ConnectionEvent evt) {}
                    public void onConnectionError(ConnectionEvent evt) {}
                });
            }
        }
      }
    }

    else if(evt.getMessage().matches("^!removebot @"+nick+" &[A-Za-z0-9]+$")) {
      Pattern r = Pattern.compile("^!removebot @"+nick+" &([A-Za-z0-9]+)$");
      Matcher m = r.matcher(evt.getMessage());
      if (m.find()) {
        boolean removed = false;
        if(hasDataFile) {
          
          synchronized(dataFile){
            JsonObject data = dataFile.getJson();
            for(int i=0;i<data.getAsJsonArray("rooms").size();i++){
              if(data.getAsJsonArray("rooms").get(i).getAsString().equals(m.group(1))&&!m.group(1).equals("bots")){
                data.getAsJsonArray("rooms").remove(i);
                removed = true;
              }
            }
            if(removed) {
              try {
                dataFile.setJson(data);
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          }
        }

        if(bot.isConnected(m.group(1))||bot.isPending(m.group(1))){
          bot.disconnectRoom(m.group(1));
          removed = true;
        }

        if(removed) {
          bot.reply(evt,"/me has been removed from &"+m.group(1),null);
        } else {
          bot.reply(evt,"/me is not in &"+m.group(1),null);
        }
      }
    }
    
    /*  PASSWORD CODE: Removed due to potential security issues.
      else if(evt.getMessage().matches("^!trypass @"+nick+" &[A-Za-z0-9]+ [\\S\\s]+$")) {
      Pattern r = Pattern.compile("^!trypass @"+nick+" &([A-Za-z0-9]+) ([\\S\\s]+)$");
      final Matcher m = r.matcher(evt.getMessage());
      final MessageEvent event = evt;
      if (m.find()) {
        try {
          RoomConnection rmCon = bot.getRoomConnection(m.group(1));
          if(rmCon.isBounced()){
            rmCon.tryPassword(m.group(2), new ReplyEventListener() {
                @Override
                public void onReplyEvent(PacketEvent arg0) {}
                @Override
                public void onReplyFail(PacketEvent arg0) {
                  event.reply("The password is incorrect.");
                }
                @Override
                public void onReplySuccess(PacketEvent arg0) {
                  event.reply("/me has been added to &"+m.group(1));
                }
            });
          } else {
            reply(evt,"/me is already in &"+m.group(1),null);
          }
        } catch (RoomNotConnectedException e) {
          reply(evt,"/me could not find connection to &"+m.group(1),null);
        }
      }
    }*/
  }
  
  @Override
  public void onCommand(String message) {
    if(message.matches("^!sendbot @"+nick+" &[A-Za-z0-9]+$")) {
      Pattern r = Pattern.compile("^!sendbot @"+nick+" &([A-Za-z0-9]+)$");
      final Matcher m = r.matcher(message);
      if (m.find()) {
        if(bot.isConnected(m.group(1))||bot.isPending(m.group(1))||bot.isStructurePending(m.group(1))||bot.isCollapsed(m.group(1))){
          boolean isBounced = false;
          try {
            isBounced = bot.getRoomConnection(m.group(1)).isBounced();
          } catch (RoomNotConnectedException e1) {
            e1.printStackTrace();
          }
          if(isBounced){
            System.out.println("&"+m.group(1)+" is private.\n"
                                 +"Use \"!trypass @"+nick+" &"+m.group(1)+" [password]\" to attempt to connect with a password.");
          } else if(hasDataFile) {
            boolean isStored = false;
            for(int i=0;i<dataFile.getJson().getAsJsonArray("rooms").size();i++){
              if(dataFile.getJson().getAsJsonArray("rooms").get(i).getAsString().equals(m.group(1))&&!m.group(1).equals("bots")){
                isStored = true;
              }
            }
            if(!isStored) {
              synchronized(dataFile){
                JsonObject data = dataFile.getJson();
                data.getAsJsonArray("rooms").add(m.group(1));
                try {
                  dataFile.setJson(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
              }
              System.out.println(nick+" has been added to &"+m.group(1));
            } else {
              System.out.println(nick+" is already in &"+m.group(1));
            }
          }
        } else {
            if(bot.parent==null){
              connectRoomConsole(m.group(1));
            } else if(bot.parent.isConnected(m.group(1))){
              connectRoomConsole(m.group(1));
            } else if(bot.parent.isPending(m.group(1))) {
                try {
                    bot.parent.getAnyRoomConnection(m.group(1)).listeners.add(ConnectionEventListener.class,  new ConnectionEventListener() {
                            @Override
                            public void onConnect(ConnectionEvent evt) {
                              connectRoomConsole(m.group(1));
                            }
                            public void onDisconnect(ConnectionEvent evt) {}
                            public void onConnectionError(ConnectionEvent evt) {}
                        });
                } catch (RoomNotConnectedException e) {/* Should never happen */}
            } else {
                bot.parent.connectStructureToRoom(m.group(1), new ConnectionEventListener() {
                    @Override
                    public void onConnect(ConnectionEvent evt) {
                        connectRoomConsole(m.group(1));
                    }
                    public void onDisconnect(ConnectionEvent evt) {}
                    public void onConnectionError(ConnectionEvent evt) {}
                });
            }
          
        }
      }
    }


    else if(message.matches("^!removebot @"+nick+" &[A-Za-z0-9]+$")) {
      Pattern r = Pattern.compile("^!removebot @"+nick+" &([A-Za-z0-9]+)$");
      Matcher m = r.matcher(message);
      if (m.find()) {
        boolean removed = false;
        if(hasDataFile) {
          
          synchronized(dataFile){
            JsonObject data = dataFile.getJson();
            for(int i=0;i<data.getAsJsonArray("rooms").size();i++){
              if(data.getAsJsonArray("rooms").get(i).getAsString().equals(m.group(1))&&!m.group(1).equals("bots")){
                data.getAsJsonArray("rooms").remove(i);
                removed = true;
              }
            }
            if(data.getAsJsonObject("room-passwords").has(m.group(1))){
              data.getAsJsonObject("room-passwords").remove(m.group(1));
            }
            if(removed) {
              try {
                dataFile.setJson(data);
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          }
        }

        if(bot.isConnected(m.group(1))||bot.isPending(m.group(1))){
          bot.disconnectRoom(m.group(1));
          removed = true;
        }

        if(removed) {
          System.out.println(nick+" has been removed from &"+m.group(1));
        } else {
          System.out.println(nick+" is not in &"+m.group(1));
        }
      }
    }
    
    
    else if(message.matches("^!trypass @"+nick+" &[A-Za-z0-9]+ [\\S\\s]+$")) {
      Pattern r = Pattern.compile("^!trypass @"+nick+" &([A-Za-z0-9]+) ([\\S\\s]+)$");
      final Matcher m = r.matcher(message);
      if (m.find()) {
        try {
          RoomConnection rmCon = bot.getRoomConnection(m.group(1));
          if(rmCon.isBounced()){
            rmCon.tryPassword(m.group(2), new ReplyEventListener() {
              public void onReplyEvent(PacketEvent arg0) {}

              @Override
              public void onReplyFail(PacketEvent arg0) {
                System.out.println("The password to &"+m.group(1)+" is incorrect.");
              }
              @Override
              public void onReplySuccess(PacketEvent arg0) {
                System.out.println("Password accepted. "+nick+" has been added to &"+m.group(1));
                synchronized(dataFile){
                  if(hasDataFile) {
                    boolean isStored = false;
                    for(int i=0;i<dataFile.getJson().getAsJsonArray("rooms").size();i++){
                      if(dataFile.getJson().getAsJsonArray("rooms").get(i).getAsString().equals(m.group(1))&&!m.group(1).equals("bots")){
                        isStored = true;
                      }
                    }
                    if(!isStored) {
                      JsonObject data = dataFile.getJson();
                      data.getAsJsonArray("rooms").add(m.group(1));
                      try {
                        dataFile.setJson(data);
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
                    }
                  }
                  JsonObject data = dataFile.getJson();
                  if(!data.getAsJsonObject("room-passwords").has(m.group(1))){
                    data.getAsJsonObject("room-passwords").addProperty(m.group(1), m.group(2));
                    try {
                      dataFile.setJson(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                  }
                }
              }
            });
          } else {
            System.out.println(nick+" is already in &"+m.group(1));
          }
        } catch (RoomNotConnectedException e) {
          System.out.println(nick+" could not find connection to &"+m.group(1));
        }
      }
    }
  }
  
  
  // Connect to room from init
  private void connectRoomInit(final JsonObject data, final String room){
      RoomConnection c = bot.createRoomConnection(room);
      
      c.listeners.add(PacketEventListener.class, new PacketEventListener() {
        @Override
        public void onBounceEvent(PacketEvent evt) {
          if(data.getAsJsonObject("room-passwords").has(room)){
            try {
              RoomConnection rmCon = bot.getRoomConnection(room);
              if(rmCon.isBounced()){
                rmCon.tryPassword(data.getAsJsonObject("room-passwords").get(room).getAsString(), new ReplyEventListener() {
                    @Override
                    public void onReplyEvent(PacketEvent arg0) {}
                    @Override
                    public void onReplyFail(PacketEvent arg0) {
                      System.out.println("The password for &"+room+" supplied in the data file is incorrect.");
                    }
                    @Override
                    public void onReplySuccess(PacketEvent arg0) {
                      //Connected successfully
                      System.out.println("Password accepted. "+nick+" has been added to &"+room);
                    }
                });
              } else {
                //Already connected
              }
            } catch (RoomNotConnectedException e) {
              //Couldn't find connection
            }
          } else {
            System.out.println("&"+room+" is private.\n"
                               +"Use \"!trypass @"+nick+" &"+room+" [password]\" to attempt to connect with a password.");
          }
        }
        public void onHelloEvent(PacketEvent arg0) {}
        public void onJoinEvent(PacketEvent arg0) {}
        public void onNickEvent(PacketEvent arg0) {}
        public void onPartEvent(PacketEvent arg0) {}
        public void onSendEvent(MessageEvent arg0) {}
        public void onSnapshotEvent(PacketEvent arg0) {}
        public void packetRecieved(PacketEvent evt) {}
      });
      
      bot.startRoomConnection(c);
  }
  
  // Connect to room from euphoria
  private void connectRoomEuphoria(final String room, final MessageEvent event){
      RoomConnection c = bot.createRoomConnection(room);
          event.reply("/me is attempting to join...");
          
          c.listeners.add(ConnectionEventListener.class,new ConnectionEventListener(){
              @Override
              public void onConnectionError(ConnectionEvent arg0) {
                event.reply("/me could not find &"+room);
              }
              public void onConnect(ConnectionEvent arg0) {}
              public void onDisconnect(ConnectionEvent arg0) {}
          });
          
          c.listeners.add(PacketEventListener.class, new PacketEventListener() {
            @Override
            public void onBounceEvent(PacketEvent evt) {
              /*event.reply("&"+matcher.group(1)+" is private. "
                          +"Use \"!trypass @"+nick+" &"+matcher.group(1)+" [password]\" to attempt to connect.\n"
                          +"Please be aware that this will be visible to anyone in this room.");*/
              event.reply("&"+room+" is private.");
              evt.getRoomConnection().closeConnection("Room is private.");
            }
            
            @Override
            public void onSnapshotEvent(PacketEvent evt) {
              event.reply("/me has been added to &"+room);
              if(hasDataFile) {
                boolean isStored = false;
                for(int i=0;i<dataFile.getJson().getAsJsonArray("rooms").size();i++){
                  if(dataFile.getJson().getAsJsonArray("rooms").get(i).getAsString().equals(room)&&!room.equals("bots")){
                    isStored = true;
                  }
                }
                if(!isStored) {
                  synchronized(dataFile){
                    JsonObject data = dataFile.getJson();
                    data.getAsJsonArray("rooms").add(room);
                    try {
                      dataFile.setJson(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                  }
                }
              }
            }
            public void onHelloEvent(PacketEvent arg0) {}
            public void onJoinEvent(PacketEvent arg0) {}
            public void onNickEvent(PacketEvent arg0) {}
            public void onPartEvent(PacketEvent arg0) {}
            public void onSendEvent(MessageEvent arg0) {}
            public void packetRecieved(PacketEvent evt) {}
          });
          
          bot.startRoomConnection(c);
  }
  
  // Connect to room from console
  private void connectRoomConsole(final String room){
      RoomConnection c = bot.createRoomConnection(room);
      System.out.println(nick+" is attempting to join...");
      
      c.listeners.add(ConnectionEventListener.class,new ConnectionEventListener(){
        @Override
        public void onConnectionError(ConnectionEvent arg0) {
          System.out.println(nick+" could not find &"+room);
        }
        public void onConnect(ConnectionEvent arg0) {}
        public void onDisconnect(ConnectionEvent evt) {}
      });
      
      c.listeners.add(PacketEventListener.class, new PacketEventListener() {
        @Override
        public void onBounceEvent(PacketEvent evt) {
          System.out.println("&"+room+" is private.\n"
                             +"Use \"!trypass @"+nick+" &"+room+" [password]\" to attempt to connect with a password.");
        }
        
        @Override
        public void onSnapshotEvent(PacketEvent arg0) {
          System.out.println(nick+" has been added to &"+room);
          if(hasDataFile) {
            boolean isStored = false;
            synchronized(dataFile){
              for(int i=0;i<dataFile.getJson().getAsJsonArray("rooms").size();i++){
                if(dataFile.getJson().getAsJsonArray("rooms").get(i).getAsString().equals(room)&&!room.equals("bots")){
                  isStored = true;
                }
              }
              if(!isStored) {
                JsonObject data = dataFile.getJson();
                data.getAsJsonArray("rooms").add(room);
                try {
                  dataFile.setJson(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
              }
            }
          }
        }
        public void onHelloEvent(PacketEvent arg0) {}
        public void onJoinEvent(PacketEvent arg0) {}
        public void onNickEvent(PacketEvent arg0) {}
        public void onPartEvent(PacketEvent arg0) {}
        public void onSendEvent(MessageEvent arg0) {}
        public void packetRecieved(PacketEvent evt) {}
      });
      
      bot.startRoomConnection(c);
  }
  
  public void onHelloEvent(PacketEvent arg0) {}
  public void onJoinEvent(PacketEvent arg0) {}
  public void onNickEvent(PacketEvent arg0) {}
  public void onPartEvent(PacketEvent arg0) {}
  public void onSnapshotEvent(PacketEvent arg0) {}
  public void onBounceEvent(PacketEvent arg0) {}
  public void packetRecieved(PacketEvent evt) {}
}
