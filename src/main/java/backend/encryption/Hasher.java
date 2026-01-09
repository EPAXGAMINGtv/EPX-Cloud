package backend.encryption;

import backend.logger.Logger;

public class Hasher {
    public static String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            Logger.info("hased passwort succesfully");
            return sb.toString();
        } catch (Exception e) {
            Logger.error("failed to hash passwort");
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }
}
