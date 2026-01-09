package backend;
import backend.Manager.DBManager;
import backend.Server.*;
import backend.filemanager.FileManager;
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
    private  static FileManager fmgr;

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
        HtmlContext logout = new HtmlContext("/logout",HtmlDoc.scan("html/logout.html"));
        server.addContext(logout);
        UploadContext uploadContext =  new UploadContext();
        server.addContext(uploadContext);
        DownloadContext downloadContext = new DownloadContext();
        server.addContext(downloadContext);
        FilesContext filesContext = new FilesContext();
        server.addContext(filesContext);
        RegisterContext registerContext = new RegisterContext(HtmlDoc.scan("html/register.html"));
        server.addContext(registerContext);
        LoginContext logincontext =  new LoginContext(HtmlDoc.scan("html/login.html"));
        server.addContext(logincontext);
        HomeContext homeContext = new HomeContext(HtmlDoc.scan("html/home.html"));
        server.addContext(homeContext);
        fmgr = new FileManager(dbManager);
        fmgr.start("cloudfiles");
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
                Logger.close();
                System.exit(0);
            }else if(command.equalsIgnoreCase("restart")){
                stopServer();
                restartApplication();
            }else {
                Logger.info("invalid command :"+command+" !");
            }
        }
        stopServer();
    }
    private static void restartApplication() {
        try {
            String javaBin = System.getProperty("java.home") + "/bin/java";
            String classPath = System.getProperty("java.class.path");
            String className = CloudServerMain.class.getName();

            ProcessBuilder builder = new ProcessBuilder(
                    javaBin, "-cp", classPath, className
            );
            builder.start();

            stopServer();
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void stopServer(){
        Logger.info("server stoping soon!");
        server.stopServer();
        dbManager.disconectFromDB();
        running =false;
        Logger.info("server Marked as offline!");
    }

    public  static PropertiesManager getServerpropetiesmgr(){
        return serverpropetiesmgr;
    }

    public static DBManager getDBManager(){
        return  dbManager;
    }
}
