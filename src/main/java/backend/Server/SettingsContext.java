package backend.Server;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class SettingsContext implements ServerContext{
    @Override
    public String getPath() {
        return "/settings";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

    }
}
