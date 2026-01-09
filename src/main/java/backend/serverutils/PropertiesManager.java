package backend.serverutils;

import backend.logger.Logger;

import java.io.*;
import java.util.Properties;

public class PropertiesManager {

    private final Properties properties = new Properties();
    private final File file;

    public PropertiesManager(String fileName) {
        this.file = new File(fileName);
    }

    public void load()  {
        Logger.info("loading propeties file");
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                Logger.error("error by loading the propeties");
                e.printStackTrace();
                System.exit(-1);
            }
        } else {
            setDefaults();
            try {
                save();
            } catch (IOException e) {
                Logger.error("error by saving propeties!");
                e.printStackTrace();
                System.exit(-1);
            }
            Logger.info("propeties file did not exist created now");
        }
    }

    private void setDefaults() {
        Logger.info("setting default properties");
        properties.setProperty("port", "80");
        properties.setProperty("database_name","here your databasename");
        properties.setProperty("database_port","here your database port");
        properties.setProperty("database_user","add here your database user");
        properties.setProperty("database_pw","here you databse passwort");
        properties.setProperty("database_ip","here your ip addres for your db");
    }

    public void save() throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            properties.store(fos, "Server Properties");
        }
    }

    public String getString(String key) {
        return properties.getProperty(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    public void set(String key, String value) {
        properties.setProperty(key, value);
    }
}
