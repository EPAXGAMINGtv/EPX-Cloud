package backend.Server;

import backend.logger.Logger;
import backend.serverutils.PropertiesManager;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class EpxHTTPServer {

    private HttpServer server;
    private final Map<String, ServerContext> contexts = new HashMap<>();
    private static PropertiesManager manager;

    public EpxHTTPServer(PropertiesManager mgr) {
        Logger.info("creating HTTP Server ...");
        manager = mgr;
        int port = manager.getInt("port");
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            Logger.error("can't create HTTP Server");
            e.printStackTrace();
        }
        Logger.info("starting HTTP Server ...");
        this.server.start();
    }

    public void addContext(ServerContext context) {
        String path = context.getPath();

        if (contexts.containsKey(path)) {
            Logger.error("context allready exists!");
        }

        contexts.put(path, context);
        server.createContext(path, context);

        Logger.info("Context registert: " + path);
    }


    public void stopServer(){
        Logger.info("stopping server in 10 seconds...");
        try{
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Logger.error("error by sleeping before shutdown! the server will now shutdown imidietly!");
            server.stop(0);
            e.printStackTrace();
            Logger.info("server bruce stoped");
            System.exit(-1);
        }
        Logger.info("stoping server!");
        server.stop(0);
        Logger.info("Server stopped");
    }

    public void setIcon(String pathToPngFile) {
        server.createContext("/favicon.ico", exchange -> {
            try {
                byte[] iconBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(pathToPngFile));
                exchange.getResponseHeaders().set("Content-Type", "image/png");
                exchange.sendResponseHeaders(200, iconBytes.length);
                exchange.getResponseBody().write(iconBytes);
                exchange.close();
                Logger.info("PNG favicon served from: " + pathToPngFile);
            } catch (IOException e) {
                Logger.error("Failed to load PNG favicon from: " + pathToPngFile);
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        });
    }



}
