package backend.Server;

import backend.login.Session;
import backend.login.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MoveContext implements ServerContext {

    @Override
    public String getPath() {
        return "/move";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            exchange.sendResponseHeaders(401, -1);
            return;
        }

        String sessionId = null;
        for (String c : cookieHeader.split(";")) {
            if (c.trim().startsWith("SESSION=")) {
                sessionId = c.trim().substring(8);
                break;
            }
        }

        Session session = sessionId == null ? null : SessionManager.getSession(sessionId);
        if (session == null) {
            exchange.sendResponseHeaders(401, -1);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.contains("from=") || !query.contains("to=")) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        String from = "";
        String to = "";

        for (String p : query.split("&")) {
            if (p.startsWith("from=")) from = URLDecoder.decode(p.substring(5), StandardCharsets.UTF_8);
            if (p.startsWith("to=")) to = URLDecoder.decode(p.substring(3), StandardCharsets.UTF_8);
        }

        if (from.contains("..") || to.contains("..")) {
            exchange.sendResponseHeaders(403, -1);
            return;
        }

        Path userDir = Path.of("cloudfiles", session.username).normalize();
        Path source = userDir.resolve(from).normalize();
        Path targetDir = to.isEmpty() ? userDir : userDir.resolve(to).normalize();

        if (!source.startsWith(userDir) || !targetDir.startsWith(userDir)) {
            exchange.sendResponseHeaders(403, -1);
            return;
        }

        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(source.getFileName());

        Files.move(source, target);

        exchange.sendResponseHeaders(200, -1);
    }
}
