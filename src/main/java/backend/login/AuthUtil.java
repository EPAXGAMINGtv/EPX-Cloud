package backend.login;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

public class AuthUtil {

    public static Session requireLogin(HttpExchange exchange) throws IOException {

        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie == null || !cookie.contains("SESSION=")) {
            redirectToLogin(exchange);
            return null;
        }

        String sessionId = cookie.split("SESSION=")[1].split(";")[0];
        Session session = SessionManager.getSession(sessionId);

        if (session == null) {
            redirectToLogin(exchange);
            return null;
        }

        return session;
    }

    private static void redirectToLogin(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Location", "/login");
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }
}
