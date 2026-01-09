package backend.filemanager;

import backend.Manager.DBManager;
import backend.logger.Logger;

import java.io.File;
import java.util.List;

public class FileManager {

    private final DBManager dbManager;

    public FileManager(DBManager dbManager){
        this.dbManager =dbManager;
    }

    public void start(String cloudfiledir){
        List<String> users = dbManager.getAllUserNamesAsList();
        File cloudfilesdir = new File(cloudfiledir);
        if(!cloudfilesdir.exists()){
            boolean cloudfir = cloudfilesdir.mkdir();
            Logger.info("cloud dir created:"+cloudfir);
        }else {
            Logger.info("cloudfiledir allready exists!");
        }

        for (String user: users){
            File userdir = new File(cloudfiledir+"/"+user);
            if (!userdir.exists()){
                boolean userdirstatus =userdir.mkdir();
                Logger.info("userdir from :"+user+"created:"+userdirstatus);
            }else {
                Logger.info("user dir from user:"+user+"allready exists");
            }
        }
    }


}
