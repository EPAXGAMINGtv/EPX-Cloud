package backend.Server;

import backend.login.Session;
import backend.login.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.stream.Stream;

public class DeleteContext implements ServerContext {

    @Override
    public String getPath() {
        return "/delete";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            redirect(exchange, "/files");
            return;
        }

        String sessionId = null;
        for (String c : cookieHeader.split(";")) {
            c = c.trim();
            if (c.startsWith("SESSION=")) {
                sessionId = c.substring("SESSION=".length());
                break;
            }
        }

        if (sessionId == null) {
            redirect(exchange, "/files");
            return;
        }

        Session session = SessionManager.getSession(sessionId);
        if (session == null) {
            redirect(exchange, "/files");
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.startsWith("file=")) {
            redirect(exchange, "/files");
            return;
        }

        String filePath = URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8);

        if (filePath.contains("..") || filePath.startsWith("/") || filePath.startsWith("\\")) {
            redirect(exchange, "/files");
            return;
        }

        Path userDir = Path.of("cloudfiles", session.username);
        Path targetPath = userDir.resolve(filePath);

        if (!targetPath.normalize().startsWith(userDir.normalize())) {
            redirect(exchange, "/files");
            return;
        }

        if (Files.exists(targetPath)) {
            if (Files.isDirectory(targetPath)) {
                deleteDirectoryRecursively(targetPath);
            } else {
                Files.delete(targetPath);
            }
        }

        String parentFolder = "";
        int lastSlash = filePath.lastIndexOf("/");
        if (lastSlash > 0) {
            parentFolder = filePath.substring(0, lastSlash);
        }

        redirect(exchange, "/files" + (parentFolder.isEmpty() ? "" : "?folder=" +
                java.net.URLEncoder.encode(parentFolder, StandardCharsets.UTF_8)));
    }

    private void deleteDirectoryRecursively(Path directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                        }
                    });
        }
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(302, 0);
        exchange.close();
    }
}