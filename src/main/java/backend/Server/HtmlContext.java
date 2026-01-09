package backend.Server;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HtmlContext implements ServerContext {

    private final String path;
    private final HtmlDoc doc;

    public HtmlContext(String path, HtmlDoc doc) {
        this.path = path;
        this.doc = doc;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        byte[] data = doc.getHtml().getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders()
                .set("Content-Type", "text/html; charset=UTF-8");

        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }
}
