package bots.admin;

import euphoria.packets.fields.SessionView;

public class UserConnection {
    private User        user;
    private SessionView session;
    private String      room;
    private boolean     verifiedId;
    private boolean     verifiedNick;
    
    protected UserConnection(User user, SessionView session, String room, boolean verifiedId, boolean verifiedNick) {
        this.user         = user;
        this.session      = session;
        this.room         = room;
        this.verifiedId   = verifiedId;
        this.verifiedNick = verifiedNick;
    }
    
    public User         getUser()         { return user;                                        }
    public SessionView  getSession()      { return session;                                     }
    public String       getRoom()         { return room;                                        }
    public boolean      hasVerifiedId()   { return verifiedId;                                  }
    public boolean      hasVerifiedNick() { return verifiedNick;                                }
    public int          getPermissions()  { return user.getRoomUser(room).getRoomPermissions(); }
}
