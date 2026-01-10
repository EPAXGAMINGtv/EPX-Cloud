package backend.Server;

import backend.login.Session;
import backend.login.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLDecoder;

public class CreateFolderContext implements ServerContext {

    @Override
    public String getPath() {
        return "/createfolder";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            redirect(exchange, "/login");
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
            redirect(exchange, "/login");
            return;
        }

        Session session = SessionManager.getSession(sessionId);
        if (session == null) {
            redirect(exchange, "/login");
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            redirect(exchange, "/files");
            return;
        }

        String folderName = null;
        String currentFolder = "";

        for (String param : query.split("&")) {
            if (param.startsWith("name=")) {
                folderName = URLDecoder.decode(param.substring(5), StandardCharsets.UTF_8);
            } else if (param.startsWith("folder=")) {
                currentFolder = URLDecoder.decode(param.substring(7), StandardCharsets.UTF_8);
            }
        }

        if (folderName == null || folderName.trim().isEmpty()) {
            redirect(exchange, "/files" + (currentFolder.isEmpty() ? "" : "?folder=" + currentFolder));
            return;
        }

        folderName = folderName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (currentFolder.contains("..") || currentFolder.startsWith("/") || currentFolder.startsWith("\\")) {
            currentFolder = "";
        }

        Path userDir = Path.of("cloudfiles", session.username);
        Path targetDir = currentFolder.isEmpty() ? userDir : userDir.resolve(currentFolder);
        
        if (!targetDir.normalize().startsWith(userDir.normalize())) {
            redirect(exchange, "/files");
            return;
        }

        Path newFolder = targetDir.resolve(folderName);
        Files.createDirectories(newFolder);

        redirect(exchange, "/files" + (currentFolder.isEmpty() ? "" : "?folder=" + 
                 java.net.URLEncoder.encode(currentFolder, StandardCharsets.UTF_8)));
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(302, 0);
        exchange.close();
    }
}