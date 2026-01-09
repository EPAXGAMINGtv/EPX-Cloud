package backend.Server;

import com.sun.net.httpserver.HttpHandler;

public interface ServerContext extends HttpHandler {

    String getPath();
}
