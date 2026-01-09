package backend.Server;

import backend.CloudServerMain;
import backend.Manager.DBManager;
import backend.login.Session;
import backend.login.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class RegisterContext implements ServerContext {

    private final HtmlDoc registerPage;
    private static DBManager dbManager;

    public RegisterContext(HtmlDoc registerPage) {
        this.registerPage = registerPage;
    }

    @Override
    public String getPath() {
        return "/register";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            byte[] data = registerPage.getHtml().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
            return;
        }

        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            String username = null;
            String password = null;

            for (String pair : body.split("&")) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    String key = kv[0];
                    String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    if (key.equals("username")) username = value.trim();
                    if (key.equals("password")) password = value;
                }
            }

            dbManager = CloudServerMain.getDBManager();

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                byte[] msg = "Username or password cannot be empty".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(400, msg.length);
                exchange.getResponseBody().write(msg);
                exchange.close();
                return;
            }

            if (dbManager.userExists(username)) {
                byte[] msg = "Username already taken".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(409, msg.length);
                exchange.getResponseBody().write(msg);
                exchange.close();
                return;
            }

            dbManager.createNewAccount(username, password);

            Session session = SessionManager.createSession(username);
            exchange.getResponseHeaders().add(
                    "Set-Cookie",
                    "SESSION=" + session.sessionId + "; Path=/; SameSite=Strict"
            );
            exchange.getResponseHeaders().add("Location", "/home");
            exchange.sendResponseHeaders(302, 0);
            exchange.close();
        }
    }
}
