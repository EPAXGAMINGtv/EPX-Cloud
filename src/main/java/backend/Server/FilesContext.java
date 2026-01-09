package backend.Server;

import backend.login.Session;
import backend.login.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilesContext implements ServerContext {

    @Override
    public String getPath() {
        return "/files";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        // --- SESSION CHECK ---
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            redirectToLogin(exchange);
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
            redirectToLogin(exchange);
            return;
        }

        Session session = SessionManager.getSession(sessionId);
        if (session == null) {
            redirectToLogin(exchange);
            return;
        }

        // --- USER DIRECTORY ---
        Path userDir = Path.of("cloudfiles", session.username);
        Files.createDirectories(userDir);

        // --- LOAD HTML TEMPLATE ---
        HtmlDoc doc = HtmlDoc.scan("html/files.html");
        String html = doc.getHtml();

        // --- BUILD FILE LIST ---
        StringBuilder list = new StringBuilder();

        try (var stream = Files.list(userDir)) {
            stream.forEach(path -> {
                String name = path.getFileName().toString();

                list.append("""
                    <div class="file-item">
                        <span class="file-name">%s</span>
                        <a href="/download?file=%s">
                            <button class="download-btn">Download</button>
                        </a>
                    </div>
                    """.formatted(name, name));
            });
        }

        if (list.isEmpty()) {
            list.append("<p>No files uploaded yet.</p>");
        }

        // --- INSERT INTO HTML ---
        html = html.replace("<!-- FILE_LIST -->", list.toString());

        // --- SEND RESPONSE ---
        byte[] data = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    private void redirectToLogin(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Location", "/login");
        exchange.sendResponseHeaders(302, 0);
        exchange.close();
    }
}
