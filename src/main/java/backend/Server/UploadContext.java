package backend.Server;

import backend.login.Session;
import backend.login.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class UploadContext implements ServerContext {

    @Override
    public String getPath() {
        return "/upload";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            handleGet(exchange);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            handlePost(exchange);
            return;
        }

        exchange.sendResponseHeaders(405, -1);
        exchange.close();
    }

    private void handleGet(HttpExchange exchange) throws IOException {

        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            redirect(exchange);
            return;
        }

        String sessionId = null;
        for (String c : cookieHeader.split(";")) {
            c = c.trim();
            if (c.startsWith("SESSION=")) {
                sessionId = c.substring(8);
                break;
            }
        }

        Session session = sessionId == null ? null : SessionManager.getSession(sessionId);
        if (session == null) {
            redirect(exchange);
            return;
        }

        byte[] html = Files.readAllBytes(Path.of("html/upload.html"));

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, html.length);
        exchange.getResponseBody().write(html);
        exchange.close();
    }

    private void handlePost(HttpExchange exchange) throws IOException {

        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }

        String sessionId = null;
        for (String c : cookieHeader.split(";")) {
            c = c.trim();
            if (c.startsWith("SESSION=")) {
                sessionId = c.substring(8);
                break;
            }
        }

        Session session = sessionId == null ? null : SessionManager.getSession(sessionId);
        if (session == null) {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String currentFolder = "";

        if (query != null && query.contains("folder=")) {
            for (String p : query.split("&")) {
                if (p.startsWith("folder=")) {
                    currentFolder = URLDecoder.decode(p.substring(7), StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        if (currentFolder.contains("..") || currentFolder.startsWith("/") || currentFolder.startsWith("\\")) {
            exchange.sendResponseHeaders(403, -1);
            exchange.close();
            return;
        }

        Path userDir = Path.of("cloudfiles", session.username);
        Path targetDir = currentFolder.isEmpty() ? userDir : userDir.resolve(currentFolder);
        Files.createDirectories(targetDir);

        String filename = exchange.getRequestHeaders().getFirst("X-Filename");
        if (filename == null || filename.isBlank()) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path filePath = targetDir.resolve(filename);

        try (
                InputStream in = exchange.getRequestBody();
                OutputStream out = Files.newOutputStream(filePath)
        ) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }

        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private void redirect(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Location", "/login");
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }
}
