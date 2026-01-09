package backend.login;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public static Session createSession(String username) {
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId, username, Set.of("USER"));
        sessions.put(sessionId, session);
        return session;
    }

    public static Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public static void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }
}
