package backend.Manager;

import backend.CloudServerMain;
import backend.encryption.Hasher;
import backend.logger.Logger;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class DBManager {
    private static String DATABASE_URL;
    private static String DATABASE_USER;
    private static String DATABASE_PW;
    private static String DATABASE_PORT;
    private static String DATABASE_NAME;
    private static String DATABASE_IP;

    private static Connection connection;
    private static PropertiesManager manager;

    public void connectToDB(){
        manager = CloudServerMain.getServerpropetiesmgr();
        DATABASE_IP = manager.getString("database_ip");
        DATABASE_NAME = manager.getString("database_name");
        DATABASE_PORT = manager.getString("database_port");
        DATABASE_USER= manager.getString("database_user");
        DATABASE_PW = manager.getString("database_pw");
        DATABASE_URL = "jdbc:mysql://"+DATABASE_IP+":"+DATABASE_PORT+"/"+DATABASE_NAME+"?useSSL=false&allowPublicKeyRetrieval=true";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DATABASE_URL,DATABASE_USER,DATABASE_PW);
            Logger.info("connected to database succesfully!");
        } catch (Exception e) {
            Logger.info("failed to connect to the database check in the propeties if everything is correct!");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static void createTable(String sql){
        if (connection !=null){
            try (Statement stmt = connection.createStatement()){
                stmt.executeUpdate(sql);
                Logger.info("Table is now ready");
            } catch (SQLException e) {
                Logger.error("failed to create Table with sql String:"+sql+" !");
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    public void createCryptionTableIfNotExists() {
        if (connection != null) {
            String createTableSql = "CREATE TABLE IF NOT EXISTS cryption (" +
                    "`encryption_key` TEXT NOT NULL" +
                    ");";

            createTable(createTableSql);
            String existingKey = get_cryptionKey();
            if (existingKey == null) {
                String newKey = generateRandomAESKey();
                set_cryptionKey(newKey);
                Logger.info("New encryption key generated and stored.");
            } else {
                Logger.info("Encryption key already exists.");
            }
        }
    }



    public void createTableIfNotExistsUserdata(){
        if (connection != null){
            String sql = "CREATE TABLE IF NOT EXISTS user_data (" +
                    "user_name VARCHAR(255) NOT NULL UNIQUE," +
                    "passwort TEXT NOT NULL" +
                    ");";
            createTable(sql);
        }
    }

    public boolean userExists(String username) {
        if (connection == null) return false;

        try {
            var ps = connection.prepareStatement(
                    "SELECT user_name FROM user_data WHERE user_name=?"
            );
            ps.setString(1, username);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void set_cryptionKey(String encryptionKey) {
        if (connection != null) {
            String sql = "INSERT INTO cryption (`encryption_key`) VALUES (?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, encryptionKey);
                stmt.executeUpdate();
                System.out.println("Encryption key successfully set");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }



    public String get_cryptionKey() {
        if (connection != null) {
            String sql = "SELECT `encryption_key` FROM cryption LIMIT 1";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getString("encryption_key");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public boolean createNewAccount(String username, String plainPassword) {
        if (connection == null) return false;

        try {
            var check = connection.prepareStatement(
                    "SELECT user_name FROM user_data WHERE user_name=?"
            );
            check.setString(1, username);

            var rs = check.executeQuery();
            if (rs.next()) {
                Logger.error("User already exists: " + username);
                return false;
            }

            var insert = connection.prepareStatement(
                    "INSERT INTO user_data (user_name, passwort) VALUES (?,?)"
            );
            insert.setString(1, username);
            insert.setString(2, Hasher.hashPassword(plainPassword));
            insert.executeUpdate();

            Logger.info("User created: " + username);
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getAllUserNamesAsList() {
        List<String> users = new ArrayList<>();

        if (connection == null) return users;

        try {
            var ps = connection.prepareStatement(
                    "SELECT user_name FROM user_data"
            );

            var rs = ps.executeQuery();
            while (rs.next()) {
                users.add(rs.getString("user_name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }


    public boolean deleteAcount(String username) {
        if (connection == null) return false;

        try {
            var ps = connection.prepareStatement(
                    "DELETE FROM user_data WHERE user_name=?"
            );
            ps.setString(1, username);

            boolean deleted = ps.executeUpdate() > 0;

            if (deleted) {
                Logger.info("Account deleted: " + username);
            } else {
                Logger.error("Account not found: " + username);
            }

            return deleted;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkForLogin(String username, String plainPassword) {
        if (connection == null) return false;

        try {
            var ps = connection.prepareStatement(
                    "SELECT passwort FROM user_data WHERE user_name=?"
            );
            ps.setString(1, username);

            var rs = ps.executeQuery();
            if (!rs.next()) {
                return false;
            }
            String storedHash = rs.getString("passwort");
            String inputHash = Hasher.hashPassword(plainPassword);

            return storedHash.equals(inputHash);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }



    public boolean getUsername(String username) {
        try {
            var ps = connection.prepareStatement(
                    "SELECT user_name FROM user_data WHERE user_name=?"
            );
            ps.setString(1, username);

            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setUsername(String oldName, String newName) {
        try {
            var ps = connection.prepareStatement(
                    "UPDATE user_data SET user_name=? WHERE user_name=?"
            );
            ps.setString(1, newName);
            ps.setString(2, oldName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getPwHash(String username) {
        try {
            var ps = connection.prepareStatement(
                    "SELECT passwort FROM user_data WHERE user_name=?"
            );
            ps.setString(1, username);

            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("passwort");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean setPwHash(String username, String newPlainPassword) {
        try {
            var ps = connection.prepareStatement(
                    "UPDATE user_data SET passwort=? WHERE user_name=?"
            );
            ps.setString(1, Hasher.hashPassword(newPlainPassword));
            ps.setString(2, username);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static void disconectFromDB(){
        try {
            if (!connection.isClosed()){
                connection.close();
            }
            Logger.info("disconected from database succesfully!");
        } catch (SQLException e) {
            Logger.error("failed to disconect from database");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private String generateRandomAESKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
