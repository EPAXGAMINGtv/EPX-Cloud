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
        var ref = new Object() { String currentFolder = ""; };
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
                try {
                    String name = path.getFileName().toString();
                    boolean isDirectory = Files.isDirectory(path);
                    String relativePath = ref.currentFolder.isEmpty() ? name : ref.currentFolder + "/" + name;
                    String encodedPath = URLEncoder.encode(relativePath, StandardCharsets.UTF_8);
                    String icon = getFileIcon(name, isDirectory);


                    String sizeText;
                    if (isDirectory) {
                        sizeText = formatSize(calculateFolderSize(path));
                    } else {
                        sizeText = formatSize(Files.size(path));
                    }

                    if (isDirectory) {
                        list.append("""
                            <div class="file-item" draggable="true" data-path="%s" data-is-folder="true" style="background: rgba(77,163,255,0.12);">
                                <a href="/files?folder=%s"
                                   style="flex: 1; text-decoration: none; color: #4da3ff; font-weight: 600;
                                          display: flex; justify-content: space-between; align-items: center;">
                                    <span>📁 <span class="file-name">%s</span></span>
                                    <span style="color: #9bbfe5; font-size: 13px;">%s</span>
                                </a>
                                <div class="menu-wrapper">
                                    <button class="menu-btn">⋮</button>
                                    <div class="menu">
                                        <a href="#" onclick="event.preventDefault(); renameItem('%s', '%s'); return false;">Rename</a>
                                        <a href="/delete?file=%s" class="delete">Delete Folder</a>
                                    </div>
                                </div>
                            </div>
                            """.formatted(
                                encodedPath,
                                encodedPath,
                                name,
                                sizeText,
                                encodedPath,
                                name.replace("'", "\\'"),
                                encodedPath
                        ));
                    } else {
                        String nameLower = name.toLowerCase();
                        boolean isImage = nameLower.endsWith(".png") || nameLower.endsWith(".jpg")
                                || nameLower.endsWith(".jpeg") || nameLower.endsWith(".bmp")
                                || nameLower.endsWith(".gif") || nameLower.endsWith(".webp");

                        if (isImage) icon = "🖼️";

                        list.append("""
                            <div class="file-item" draggable="true" data-path="%s" data-is-folder="false">
                                <span class="file-name clickable" onclick="%s"
                                      style="cursor: pointer; flex: 1;
                                             display: flex; justify-content: space-between; align-items: center;">
                                    <span>%s %s</span>
                                    <span style="color: #9bbfe5; font-size: 13px;">%s</span>
                                </span>
                                <div class="menu-wrapper">
                                    <button class="menu-btn">⋮</button>
                                    <div class="menu">
                                        %s
                                    </div>
                                </div>
                            </div>
                            """.formatted(
                                encodedPath,
                                isImage ? "openImageViewer('/image-preview?file=" + encodedPath + "')" : "",
                                icon,
                                name,
                                sizeText,
                                isImage ?
                                        """
                                        <a href="#" onclick="event.preventDefault(); openImageViewer('/image-preview?file=%s'); return false;">View Image</a>
                                        <a href="#" onclick="event.preventDefault(); renameItem('%s', '%s'); return false;">Rename</a>
                                        <a href="/download?file=%s">Download</a>
                                        <a href="/delete?file=%s" class="delete">Delete</a>
                                        """.formatted(encodedPath, encodedPath, name.replace("'", "\\'"), encodedPath, encodedPath)
                                        :
                                        """
                                        <a href="#" onclick="event.preventDefault(); renameItem('%s', '%s'); return false;">Rename</a>
                                        <a href="/download?file=%s">Download</a>
                                        <a href="/delete?file=%s" class="delete">Delete</a>
                                        """.formatted(encodedPath, name.replace("'", "\\'"), encodedPath, encodedPath)
                        ));
                    }
                } catch (IOException ignored) {}
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

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }

    private static long calculateFolderSize(Path folder) {
        try (var walk = Files.walk(folder)) {
            return walk
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static String getFileIcon(String fileName, boolean isDirectory) {
        if (isDirectory) return "📁";

        String name = fileName.toLowerCase();

        if (name.endsWith(".iso")) return "💿";

        if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".mov"))
            return "🎬";

        if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac") || name.endsWith(".aac"))
            return "🎵";

        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".bmp") || name.endsWith(".gif") || name.endsWith(".webp"))
            return "🖼️";

        if (name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") || name.endsWith(".tar")||name.endsWith(".jar"))
            return "️\uD83D\uDCE6";

        if (name.endsWith(".exe")||name.endsWith(".py")||name.endsWith(".elf")||name.endsWith(".bin")||name.endsWith(".apk")||name.endsWith(".msi"))
            return "⚙\uFE0F";
        return "📄";
    }

}
