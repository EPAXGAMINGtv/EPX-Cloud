package backend.Server;

import backend.CloudServerMain;
import backend.Manager.DBManager;
import backend.logger.Logger;
import backend.login.Session;
import backend.login.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SettingsContext implements ServerContext {

    private final HtmlDoc settingsPage;
    private final DBManager dbManager;

    public SettingsContext(HtmlDoc settingsPage) {
        this.settingsPage = settingsPage;
        this.dbManager = CloudServerMain.getDBManager();
    }

    @Override
    public String getPath() {
        return "/settings";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Session session = getSession(exchange);
        if (session == null) {
            exchange.getResponseHeaders().add("Location", "/login");
            exchange.sendResponseHeaders(302, 0);
            exchange.close();
            return;
        }

        if (exchange.getRequestMethod().equalsIgnoreCase("GET") && path.equals("/settings")) {
            byte[] data = settingsPage.getHtml().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
            return;
        }

        if (exchange.getRequestMethod().equalsIgnoreCase("POST") && path.equals("/settings/change-password")) {
            Map<String, String> form = parseForm(exchange);
            String oldPassword = form.get("oldPassword");
            String newPassword = form.get("newPassword");

            if (oldPassword == null || newPassword == null || oldPassword.isEmpty() || newPassword.isEmpty()) {
                sendText(exchange, 400, "All fields are required");
                return;
            }

            boolean passwordCorrect = dbManager.checkForLogin(session.username, oldPassword);
            if (!passwordCorrect) {
                sendText(exchange, 403, "Incorrect old password");
                return;
            }

            boolean ok = dbManager.setPwHash(session.username, newPassword);
            if (!ok) {
                Logger.info("password change failed for user " + session.username);
                sendText(exchange, 403, "Password change failed");
                return;
            }

            Logger.info("password changed for user " + session.username);
            sendText(exchange, 200, "Password changed successfully");
            return;
        }

        if (exchange.getRequestMethod().equalsIgnoreCase("POST") && path.equals("/settings/delete-account")) {
            File userfiles = new File("cloudfiles/"+session.username);
            boolean deletedfiles = userfiles.delete();
            if (!deletedfiles){
                sendText(exchange,500,"failed to delete your data");
            }else {
                dbManager.deleteAcount(session.username);
            }


            SessionManager.removeSession(session.sessionId);
            exchange.getResponseHeaders().add("Set-Cookie", "SESSION=; Path=/; Max-Age=0");
            sendText(exchange, 200, "Account deleted successfully");
            return;
        }

        exchange.sendResponseHeaders(404, -1);
        exchange.close();
    }

    private Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> map = new HashMap<>();

        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
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

    private void sendText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }
}
