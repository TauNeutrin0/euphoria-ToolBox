package bots.admin;

import java.util.HashMap;
import java.util.Map;

import bots.tools.StructuredBot;
import euphoria.FileIO;
import euphoria.packets.fields.SessionView;

public class UserBot extends StructuredBot {
    FileIO dataFile;
    Map<String, Map<String, User>> connectedUsers = new HashMap<String, Map<String, User>>();
    Map<String, User> users = new HashMap<String, User>();
    
    public User getUser(String id) {
        return users.get(id);
    }
    
    public UserConnection getUserConnection(String room, SessionView session) {
        User user = getUserByUserId(room, session.getId());
        if(user!=null){
            return new UserConnection(user, session, room, true, user.getRoomUser(room).usesAlias(session.getName()));
        } else {
            user = getUserByNick(room, session.getName());
            if(user!=null){
                return new UserConnection(user, session, room, false, true);
            }
        }
        return null;
    }
    
    public User getUserByNick(String room, String nick) {
        for(Map.Entry<String, User> entry : users.entrySet()) {
            User user = entry.getValue();
            if(user.getRoomUser(room).usesAlias(nick)) return user;
        }
        return null;
    }
    
    public User getUserByUserId(String room, String userId) {
        for(Map.Entry<String, User> entry : users.entrySet()) {
            User user = entry.getValue();
            if(user.getRoomUser(room).usesUserId(userId)) return user;
        }
        return null;
    }
    
    public UserBot(StructuredBot parent) {
        super(parent,"UserBot");
        //Load users
    }
}
