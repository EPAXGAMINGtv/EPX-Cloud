package backend;
import backend.Manager.DBManager;
import backend.Server.*;
import backend.logger.Logger;
import backend.serverutils.PropertiesManager;
import com.sun.net.httpserver.HttpContext;

import java.util.Scanner;

public class CloudServerMain {
    //server main class
    public static boolean running = false;
    private static PropertiesManager serverpropetiesmgr;
    private static EpxHTTPServer server;
    private static DBManager dbManager;

    public static void main(String[] args){
        Logger.info("starting EpxCloudServer...");
        serverpropetiesmgr = new PropertiesManager("server.propeties");
        serverpropetiesmgr.load();
        dbManager = new DBManager();
        dbManager.connectToDB();
        dbManager.createTableIfNotExistsUserdata();
        dbManager.createNewAccount("epx","test123");
        server = new EpxHTTPServer(serverpropetiesmgr);
        server.setIcon("html/assets/icon.png");
        HtmlContext index = new HtmlContext("/",HtmlDoc.scan("html/index.html"));
        server.addContext(index);
        //
        RegisterContext registerContext = new RegisterContext(HtmlDoc.scan("html/register.html"));
        server.addContext(registerContext);
        LoginContext logincontext =  new LoginContext(HtmlDoc.scan("html/login.html"));
        server.addContext(logincontext);
        HomeContext homeContext = new HomeContext(HtmlDoc.scan("html/home.html"));
        server.addContext(homeContext);
        Scanner input = new Scanner(System.in);
        running =true;
        if (running) {
            Logger.info("server marked as running!");
        }else {
            Logger.error("there is a problem by starting up the server");
            server.stopServer();
            System.exit(-1);
        }
        while (running){
            System.out.print("EPX-CLOUD> ");
            String command = input.nextLine();
            if (command.equalsIgnoreCase("stop")){
                stopServer();
            }
        }
        stopServer();
    }

    public static void stopServer(){
        Logger.info("server stoping soon!");
        server.stopServer();
        dbManager.disconectFromDB();
        running =false;
        Logger.info("server Marked as offline!");
        Logger.close();
        System.exit(0);
    }

    public  static PropertiesManager getServerpropetiesmgr(){
        return serverpropetiesmgr;
    }

    public static DBManager getDBManager(){
        return  dbManager;
    }
}
