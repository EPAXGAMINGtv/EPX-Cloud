package backend.Server;

import backend.login.Session;
import backend.login.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class UploadContext implements ServerContext {

    @Override
    public String getPath() {
        return "/upload";
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

        if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            String html = HtmlDoc.scan("html/upload.html").getHtml();
            byte[] data = html.getBytes();
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
            return;
        }

        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            InputStream is = exchange.getRequestBody();
            byte[] fileBytes = is.readAllBytes();
            String filename = "upload.txt";
            String cd = exchange.getRequestHeaders().getFirst("Content-Disposition");

            if (cd != null && cd.contains("filename=")) {
                filename = cd.split("filename=")[1].replace("\"", "");
            }
            Path userDir = Path.of("cloudfiles", session.username);
            Files.createDirectories(userDir);

            Path filePath = userDir.resolve(filename);
            Files.write(filePath, fileBytes);

            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        }
    }
}
