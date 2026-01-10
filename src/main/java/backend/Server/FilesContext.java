package backend.Server;

import backend.login.Session;
import backend.login.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class FilesContext implements ServerContext {

    @Override
    public String getPath() {
        return "/files";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

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

        String query = exchange.getRequestURI().getQuery();
        var ref = new Object() {
            String currentFolder = "";
        };
        if (query != null && query.startsWith("folder=")) {
            ref.currentFolder = URLDecoder.decode(query.substring(7), StandardCharsets.UTF_8);
            if (ref.currentFolder.contains("..") || ref.currentFolder.startsWith("/") || ref.currentFolder.startsWith("\\")) {
                ref.currentFolder = "";
            }
        }

        Path userDir = Path.of("cloudfiles", session.username);
        Files.createDirectories(userDir);

        Path currentPath = ref.currentFolder.isEmpty() ? userDir : userDir.resolve(ref.currentFolder);

        if (!currentPath.normalize().startsWith(userDir.normalize())) {
            redirectToLogin(exchange);
            return;
        }

        Files.createDirectories(currentPath);

        HtmlDoc doc = HtmlDoc.scan("html/files.html");
        String html = doc.getHtml();

        StringBuilder breadcrumb = new StringBuilder();
        breadcrumb.append("<a href='/files' style='color: #4da3ff; text-decoration: none;'>📁 Root</a>");

        if (!ref.currentFolder.isEmpty()) {
            String[] parts = ref.currentFolder.split("/");
            String accumulatedPath = "";
            for (String part : parts) {
                if (!part.isEmpty()) {
                    accumulatedPath += (accumulatedPath.isEmpty() ? "" : "/") + part;
                    String encodedPath = URLEncoder.encode(accumulatedPath, StandardCharsets.UTF_8);
                    breadcrumb.append(" / <a href='/files?folder=").append(encodedPath)
                            .append("' style='color: #4da3ff; text-decoration: none;'>")
                            .append(part).append("</a>");
                }
            }
        }

        StringBuilder list = new StringBuilder();

        list.append("""
            <div style="margin: 12px; padding: 14px; background: rgba(77,163,255,0.15); border-radius: 10px; border: 2px dashed rgba(77,163,255,0.5);">
                <div style="display: flex; gap: 10px; align-items: center;">
                    <input type="text" id="new-folder-name" placeholder="Folder Name" style="flex: 1; padding: 10px; background: rgba(0,0,0,0.4); border: 1px solid rgba(255,255,255,0.2); border-radius: 6px; color: #fff; font-size: 14px;">
                    <button onclick="createFolder()" style="padding: 10px 20px; background: linear-gradient(135deg, #4da3ff, #66ffcc); border: none; border-radius: 6px; color: #000; font-weight: 600; cursor: pointer; font-size: 14px;">📁 Create Folder</button>
                </div>
            </div>
            """);

        try (var stream = Files.list(currentPath)) {
            stream.sorted((a, b) -> {
                boolean aIsDir = Files.isDirectory(a);
                boolean bIsDir = Files.isDirectory(b);
                if (aIsDir && !bIsDir) return -1;
                if (!aIsDir && bIsDir) return 1;
                return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
            }).forEach(path -> {
                String name = path.getFileName().toString();
                boolean isDirectory = Files.isDirectory(path);

                String relativePath = ref.currentFolder.isEmpty() ? name : ref.currentFolder + "/" + name;
                String encodedPath = URLEncoder.encode(relativePath, StandardCharsets.UTF_8);

                if (isDirectory) {
                    list.append("""
                        <div class="file-item" style="background: rgba(77,163,255,0.12);">
                            <a href="/files?folder=%s" style="flex: 1; text-decoration: none; color: #4da3ff; font-weight: 600; display: flex; align-items: center; gap: 8px;">
                                📁 <span class="file-name">%s</span>
                            </a>
                            <div class="menu-wrapper">
                                <button class="menu-btn">⋮</button>
                                <div class="menu">
                                    <a href="/delete?file=%s" class="delete">Delete Folder</a>
                                </div>
                            </div>
                        </div>
                        """.formatted(encodedPath, name, encodedPath));
                } else {
                    list.append("""
                        <div class="file-item">
                            <span class="file-name">📄 %s</span>
                            <div class="menu-wrapper">
                                <button class="menu-btn">⋮</button>
                                <div class="menu">
                                    <a href="/download?file=%s">Download</a>
                                    <a href="/delete?file=%s" class="delete">Delete</a>
                                </div>
                            </div>
                        </div>
                        """.formatted(name, encodedPath, encodedPath));
                }
            });
        }

        if (list.toString().equals("")) {
            list.append("<p style='text-align: center; color: #9bbfe5; margin-top: 40px;'>This folder is empty.</p>");
        }

        html = html.replace("<!-- BREADCRUMB -->", breadcrumb.toString());
        html = html.replace("<!-- FILE_LIST -->", list.toString());
        html = html.replace("<!-- CURRENT_FOLDER -->", ref.currentFolder);

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