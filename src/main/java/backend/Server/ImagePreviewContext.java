package backend.Server;

import backend.login.Session;
import backend.login.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

public class ImagePreviewContext implements ServerContext {

    @Override
    public String getPath() {
        return "/image-preview";
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
        if (query == null || !query.startsWith("file=")) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        // URL-decode the path
        String relativePath = URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8);

        File baseDir = new File("cloudfiles/" + session.username);
        File img = new File(baseDir, relativePath);

        // Security: Verify the file is actually inside the user's directory
        try {
            String canonicalBase = baseDir.getCanonicalPath();
            String canonicalFile = img.getCanonicalPath();

            if (!canonicalFile.startsWith(canonicalBase)) {
                exchange.sendResponseHeaders(403, -1);
                exchange.close();
                return;
            }
        } catch (IOException e) {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
            return;
        }

        if (!img.exists() || !img.isFile()) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        String mime = Files.probeContentType(img.toPath());
        if (mime == null || !mime.startsWith("image/")) {
            mime = "image/jpeg"; // fallback
        }

        byte[] data = Files.readAllBytes(img.toPath());
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
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