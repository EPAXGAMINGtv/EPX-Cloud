package backend.Server;

import backend.logger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class HtmlDoc {

    private final String html;

    private HtmlDoc(String html) {
        this.html = html;
    }

    public static HtmlDoc scan(String path) {
        try {
            String content = Files.readString(
                    Path.of(path),
                    StandardCharsets.UTF_8
            );
            return new HtmlDoc(content);
        } catch (IOException e) {
            Logger.error("can't find the required html:"+path);
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    public String getHtml() {
        return html;
    }
}
