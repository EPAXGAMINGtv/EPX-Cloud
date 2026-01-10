package backend.Server;

import backend.logger.Logger;
import backend.login.Session;
import backend.login.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class UploadContext implements ServerContext {

    @Override
    public String getPath() {
        return "/upload";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();

        if ("GET".equals(method)) {
            handleGetRequest(exchange);
            return;
        }

        if ("POST".equals(method)) {
            handlePostRequest(exchange);
            return;
        }

        sendError(exchange, 405, "Method Not Allowed");
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
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
        String currentFolder = "";
        if (query != null && query.startsWith("folder=")) {
            currentFolder = URLDecoder.decode(query.substring(7), StandardCharsets.UTF_8);
            if (currentFolder.contains("..") || currentFolder.startsWith("/") || currentFolder.startsWith("\\")) {
                currentFolder = "";
            }
        }

        HtmlDoc doc = HtmlDoc.scan("html/upload.html");
        String html = doc.getHtml();

        String folderDisplay = currentFolder.isEmpty() ? "Root" : currentFolder;
        html = html.replace("<!-- CURRENT_FOLDER -->", currentFolder);
        html = html.replace("Root", folderDisplay);

        byte[] data = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            sendError(exchange, 401, "Unauthorized");
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
            sendError(exchange, 401, "Unauthorized");
            return;
        }

        Session session = SessionManager.getSession(sessionId);
        if (session == null) {
            sendError(exchange, 401, "Unauthorized");
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String currentFolder = "";

        Logger.info("=== UPLOAD POST REQUEST DEBUG ===");
        Logger.info("Full URI: " + exchange.getRequestURI().toString());
        Logger.info("Query String: " + query);

        if (query != null && query.contains("folder=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("folder=")) {
                    currentFolder = URLDecoder.decode(param.substring(7), StandardCharsets.UTF_8);
                    break;
                }
            }

            if (currentFolder.contains("..") || currentFolder.startsWith("/") || currentFolder.startsWith("\\")) {
                currentFolder = "";
            }
        }

        Logger.info("Extracted folder: '" + currentFolder + "'");
        Logger.info("Session username: " + session.username);

        Path userDir = Path.of("cloudfiles", session.username);
        Path targetDir = currentFolder.isEmpty() ? userDir : userDir.resolve(currentFolder);

        Logger.info("User dir: " + userDir.toAbsolutePath());
        Logger.info("Target dir: " + targetDir.toAbsolutePath());

        if (!targetDir.normalize().startsWith(userDir.normalize())) {
            Logger.error("Security violation: target dir outside user dir!");
            sendError(exchange, 403, "Forbidden");
            return;
        }

        Files.createDirectories(targetDir);

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            sendError(exchange, 400, "Bad Request - Content-Type must be multipart/form-data");
            return;
        }

        String boundaryMarker = "--";
        String boundary = null;

        if (contentType.contains("boundary=")) {
            boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
            if (boundary.startsWith("\"")) {
                boundary = boundary.substring(1);
            }
            if (boundary.endsWith("\"")) {
                boundary = boundary.substring(0, boundary.length() - 1);
            }
        }

        if (boundary == null) {
            sendError(exchange, 400, "No boundary in Content-Type");
            return;
        }

        Logger.info("Boundary: " + boundary);

        InputStream input = exchange.getRequestBody();
        byte[] data = input.readAllBytes();

        Logger.info("Received data size: " + data.length + " bytes");

        int uploadCount = parseMultipartData(data, boundary, targetDir);

        Logger.info("=== UPLOAD COMPLETE ===");
        Logger.info("Total files uploaded: " + uploadCount);
        Logger.info("========================");

        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private int parseMultipartData(byte[] data, String boundary, Path targetDir) throws IOException {
        int uploadCount = 0;
        String boundaryString = "--" + boundary;
        byte[] boundaryBytes = boundaryString.getBytes(StandardCharsets.UTF_8);

        int pos = 0;

        while (pos < data.length) {
            int boundaryPos = indexOf(data, boundaryBytes, pos);
            if (boundaryPos == -1) {
                break;
            }

            pos = boundaryPos + boundaryBytes.length;

            if (pos + 2 >= data.length) {
                break;
            }

            if (data[pos] == '-' && data[pos + 1] == '-') {
                break;
            }

            while (pos < data.length && (data[pos] == '\r' || data[pos] == '\n')) {
                pos++;
            }

            int headerEnd = indexOf(data, "\r\n\r\n".getBytes(StandardCharsets.UTF_8), pos);
            if (headerEnd == -1) {
                break;
            }

            String headers = new String(data, pos, headerEnd - pos, StandardCharsets.UTF_8);
            Logger.info("Headers: " + headers);

            String filename = null;
            for (String line : headers.split("\r\n")) {
                if (line.toLowerCase().contains("content-disposition")) {
                    int filenamePos = line.indexOf("filename=\"");
                    if (filenamePos != -1) {
                        filenamePos += 10;
                        int filenameEnd = line.indexOf("\"", filenamePos);
                        if (filenameEnd != -1) {
                            filename = line.substring(filenamePos, filenameEnd);
                        }
                    }
                }
            }

            if (filename == null || filename.isEmpty()) {
                pos = headerEnd + 4;
                continue;
            }

            Logger.info("Found file: " + filename);

            filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

            pos = headerEnd + 4;

            int nextBoundary = indexOf(data, boundaryBytes, pos);
            if (nextBoundary == -1) {
                break;
            }

            int dataEnd = nextBoundary;
            while (dataEnd > pos && (data[dataEnd - 1] == '\r' || data[dataEnd - 1] == '\n')) {
                dataEnd--;
            }

            int fileSize = dataEnd - pos;
            Logger.info("File size: " + fileSize + " bytes");

            if (fileSize > 0) {
                byte[] fileData = new byte[fileSize];
                System.arraycopy(data, pos, fileData, 0, fileSize);

                Path filePath = targetDir.resolve(filename);
                Files.write(filePath, fileData);
                uploadCount++;

                Logger.info("Saved file to: " + filePath.toAbsolutePath());
            }

            pos = nextBoundary;
        }

        return uploadCount;
    }

    private int indexOf(byte[] data, byte[] pattern, int start) {
        for (int i = start; i <= data.length - pattern.length; i++) {
            boolean found = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }

    private void redirectToLogin(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Location", "/login");
        exchange.sendResponseHeaders(302, 0);
        exchange.close();
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        Logger.error("Upload error: " + message);
        byte[] response = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}