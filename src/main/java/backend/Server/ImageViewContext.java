package backend.Server;

import backend.CloudServerMain;
import backend.Manager.DBManager;
import backend.login.Session;
import backend.login.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImageViewContext implements ServerContext {

    private final HtmlDoc imageViewPage;
    private final DBManager dbManager;

    public ImageViewContext(HtmlDoc imageViewPage) {
        this.imageViewPage = imageViewPage;
        this.dbManager = CloudServerMain.getDBManager();
    }

    @Override
    public String getPath() {
        return "/imageview";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Session session = getSession(exchange);
        if (session == null) {
            exchange.getResponseHeaders().add("Location", "/login");
            exchange.sendResponseHeaders(302, 0);
            exchange.close();
            return;
        }

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        File userDir = new File("cloudfiles/" + session.username);
        if (!userDir.exists()) userDir.mkdirs();

        List<File> imagesList = new ArrayList<>();
        scanImages(userDir, imagesList);

        StringBuilder images = new StringBuilder();

        for (File f : imagesList) {
            String basePath = new File("cloudfiles/" + session.username).getAbsolutePath();
            String fullPath = f.getAbsolutePath();

            String relativePath = fullPath.substring(basePath.length());
            relativePath = relativePath.replace("\\", "/");

            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            // URL-encode the path for the src attribute
            String encodedPath = URLEncoder.encode(relativePath, StandardCharsets.UTF_8)
                    .replace("+", "%20"); // spaces should be %20 in URLs, not +

            images.append("<div class='img-box'>")
                    .append("<img src='/image-preview?file=")
                    .append(encodedPath)
                    .append("' alt='")
                    .append(escapeHtml(relativePath))
                    .append("' onerror=\"this.style.border='2px solid red';\">")
                    .append("<p>")
                    .append(escapeHtml(relativePath))
                    .append("</p>")
                    .append("</div>");
        }

        String html = imageViewPage.getHtml().replace("{{IMAGES}}", images.toString());

        byte[] data = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    private void scanImages(File dir, List<File> list) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                scanImages(f, list);
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".png") || name.endsWith(".jpg") ||
                        name.endsWith(".jpeg") || name.endsWith(".bmp") ||
                        name.endsWith(".gif") || name.endsWith(".webp")) {
                    list.add(f);
                }
            }
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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