package backend.Server;

import backend.login.AuthUtil;
import backend.login.Session;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HomeContext implements ServerContext {

    private final HtmlDoc homePage;

    public HomeContext(HtmlDoc homePage) {
        this.homePage = homePage;
    }

    @Override
    public String getPath() {
        return "/home";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        Session session = AuthUtil.requireLogin(exchange);
        if (session == null) return;
        byte[] data = homePage.getHtml().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }
}
