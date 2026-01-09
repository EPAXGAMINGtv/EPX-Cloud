package backend.login;

import java.util.Set;

public class Session {
    public final String sessionId;
    public final String username;
    public final Set<String> permissions;
    public long lastAccess;

    public Session(String sessionId, String username, Set<String> permissions) {
        this.sessionId = sessionId;
        this.username = username;
        this.permissions = permissions;
        this.lastAccess = System.currentTimeMillis();
    }
}
