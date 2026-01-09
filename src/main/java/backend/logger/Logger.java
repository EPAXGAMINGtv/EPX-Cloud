package backend.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter FILE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static BufferedWriter writer;

    public enum LogLevel {
        INFO,
        WARN,
        ERROR
    }

    static {
        try {
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            String fileName = "logs/log_" +
                    LocalDateTime.now().format(FILE_FORMAT) + ".log";

            writer = new BufferedWriter(new FileWriter(fileName, true));
            info("Logger initzialized!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void log(LogLevel level, String message) {
        String time = LocalDateTime.now().format(TIME_FORMAT);
        String line = "[" + time + "] [" + level + "] " + message;
        System.out.println(line);
        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void info(String msg) {
        log(LogLevel.INFO, msg);
    }

    public static void warn(String msg) {
        log(LogLevel.WARN, msg);
    }

    public static void error(String msg) {
        log(LogLevel.ERROR, msg);
    }
    public static void close() {
        try {
            info("Logger stopped!");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
