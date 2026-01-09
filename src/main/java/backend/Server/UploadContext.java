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

        String sessionId = null;
        for (String c : cookie.split(";")) {
            c = c.trim();
            if (c.startsWith("SESSION=")) {
                sessionId = c.substring("SESSION=".length());
                break;
            }
        }

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

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("multipart/form-data")) {
                exchange.sendResponseHeaders(400, 0);
                return;
            }

            String boundary = "--" + contentType.split("boundary=")[1];
            InputStream in = exchange.getRequestBody();
            BufferedInputStream bin = new BufferedInputStream(in);

            String line = readLine(bin);
            if (!line.startsWith(boundary)) {
                exchange.sendResponseHeaders(400, 0);
                return;
            }

            String headerLine = readLine(bin);
            String filename = "upload.bin";
            int fnIndex = headerLine.indexOf("filename=\"");
            if (fnIndex != -1) {
                int start = fnIndex + 10;
                int end = headerLine.indexOf("\"", start);
                filename = headerLine.substring(start, end);
            }

            readLine(bin);

            Path userDir = Path.of("cloudfiles", session.username);
            Files.createDirectories(userDir);
            Path filePath = userDir.resolve(filename);

            OutputStream out = Files.newOutputStream(filePath);
            byte[] buffer = new byte[8192];

            int b;
            boolean matched = false;
            ByteArrayOutputStream check = new ByteArrayOutputStream();

            while ((b = bin.read()) != -1) {
                check.write(b);
                byte[] arr = check.toByteArray();

                if (arr.length >= boundary.length() + 4) {
                    int len = arr.length;
                    if (endsWith(arr, ("\r\n" + boundary).getBytes())) {
                        matched = true;
                        out.write(arr, 0, len - (boundary.length() + 4));
                        break;
                    }
                }

                if (arr.length > boundary.length() + 4) {
                    out.write(arr[0]);
                    check.reset();
                    check.write(arr, 1, arr.length - 1);
                }
            }

            out.close();
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        }
    }

    private String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') out.write(c);
        }
        return out.toString();
    }

    private boolean endsWith(byte[] data, byte[] suffix) {
        if (data.length < suffix.length) return false;
        for (int i = 0; i < suffix.length; i++) {
            if (data[data.length - suffix.length + i] != suffix[i]) return false;
        }
        return true;
    }
}
