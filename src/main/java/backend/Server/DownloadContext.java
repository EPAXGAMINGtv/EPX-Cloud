package backend.Server;

import backend.login.Session;
import backend.login.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class DownloadContext implements ServerContext {

    @Override
    public String getPath() {
        return "/download";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie == null || !cookie.contains("SESSION=")) {
            exchange.getResponseHeaders().add("Location", "/login");
            exchange.sendResponseHeaders(302, 0);
            return;
        }

        String sessionId = cookie.split("SESSION=")[1];
        Session session = SessionManager.getSession(sessionId);
        if (session == null) {
            exchange.getResponseHeaders().add("Location", "/login");
            exchange.sendResponseHeaders(302, 0);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String filename = query.split("file=")[1];

        Path file = Path.of("cloudfiles", session.username, filename);

        if (!Files.exists(file)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        long fileSize = Files.size(file);

        String contentType = Files.probeContentType(file);
        if (contentType == null) contentType = "application/octet-stream";

        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        exchange.sendResponseHeaders(200, fileSize);

        try (InputStream in = Files.newInputStream(file);
             OutputStream out = exchange.getResponseBody()) {

            byte[] buffer = new byte[64 * 1024];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        exchange.close();
    }
}
