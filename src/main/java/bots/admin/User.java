package bots.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class User implements Comparable<User> {
    private String id;
    private String primaryNick;
    private Map<String, RoomUser> roomUsers;
    private List<String> ids = new ArrayList<String>();
    
    protected User() {
        
    }
    
    public String getId() {
        return id;
    }
    
    public String getPrimaryNick() {
        return primaryNick;
    }
    
    protected void addId(String id) {
        ids.add(id);
    }
    
    protected void removeId(String id) {
        ids.remove(id);
    }
    
    public boolean isInRoom(String room) {
        return roomUsers.containsKey(room);
    }
    
    public RoomUser getRoomUser(String room) {
        return roomUsers.get(room);
    }
    
    protected void removeFromRoom(String room) {
        roomUsers.remove(room);
    }
    
    public int compareTo(User compareUser) {

		long compareQuantity = Long.valueOf(compareUser.getId(), 36);

		return (int) (Long.valueOf(id, 36) - compareQuantity);
	}
	
	public class RoomUser {
	    private String primaryNick;
	    private List<String> aliases = new ArrayList<String>();
	    private int roomPermissions;
	    private List<String> userIds;
        
        public String getPrimaryNick() {
            return primaryNick;
        }
        
        protected void setPrimaryNick(String nick) {
            primaryNick = nick;
            if(!aliases.contains(nick)) aliases.add(nick);
        }
        
        public boolean usesUserId(String id) {
            return userIds.contains(id);
        }
        
        protected List<String> getUserIds() {
            return userIds;
        }
        
        public boolean usesAlias(String alias) {
            return aliases.contains(alias);
        }
        
        protected List<String> getAliases() {
            return aliases;
        }
	    
	    public int getRoomPermissions() {
	        return roomPermissions;
	    }
	    
	    protected void setRoomPermissions(int permissions){
	        roomPermissions = permissions;
	    }
	}
}
