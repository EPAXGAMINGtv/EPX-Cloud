package backend.Server;

import backend.login.Session;
import backend.login.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class RenameContext implements ServerContext {

    @Override
    public String getPath() {
        return "/rename";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Session session = getSession(exchange);
        if (session == null) {
            exchange.sendResponseHeaders(403, -1);
            exchange.close();
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        String oldPath = null;
        String newName = null;

        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2) {
                if (kv[0].equals("file")) {
                    oldPath = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                } else if (kv[0].equals("newname")) {
                    newName = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        }

        if (oldPath == null || newName == null || newName.trim().isEmpty()) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        // Security: prevent path traversal
        if (newName.contains("..") || newName.contains("/") || newName.contains("\\")) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        Path userDir = Path.of("cloudfiles", session.username);
        Path oldFile = userDir.resolve(oldPath);
        
        // Get parent directory and create new path
        Path parentDir = oldFile.getParent();
        if (parentDir == null) {
            parentDir = userDir;
        }
        Path newFile = parentDir.resolve(newName);

        // Security check
        if (!oldFile.normalize().startsWith(userDir.normalize()) || 
            !newFile.normalize().startsWith(userDir.normalize())) {
            exchange.sendResponseHeaders(403, -1);
            exchange.close();
            return;
        }

        if (!Files.exists(oldFile)) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        if (Files.exists(newFile)) {
            exchange.sendResponseHeaders(409, -1); // Conflict - file already exists
            exchange.close();
            return;
        }

        try {
            Files.move(oldFile, newFile);
            
            // Redirect back to the folder
            String folder = oldPath.contains("/") ? 
                oldPath.substring(0, oldPath.lastIndexOf("/")) : "";
            
            String redirectUrl = folder.isEmpty() ? "/files" : 
                "/files?folder=" + java.net.URLEncoder.encode(folder, StandardCharsets.UTF_8);
            
            exchange.getResponseHeaders().add("Location", redirectUrl);
            exchange.sendResponseHeaders(302, 0);
            exchange.close();
        } catch (IOException e) {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        }
    }

    private Session getSession(HttpExchange exchange) {
        Optional<String> cookie = Optional.ofNullable(exchange.getRequestHeaders().getFirst("Cookie"));
        if (cookie.isEmpty()) return null;

        for (String c : cookie.get().split(";")) {
            String[] kv = c.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals("SESSION")) {
                return SessionManager.getSession(kv[1]);
            }
        }
        return null;
    }
}