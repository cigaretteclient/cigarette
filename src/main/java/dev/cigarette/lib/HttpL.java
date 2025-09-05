package dev.cigarette.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

public class HttpL {
    public static void downloadUrlToFile(String url, String f) throws IOException {
        try (java.io.InputStream in = URI.create(url).toURL().openStream()) {
            byte[] b = in.readAllBytes();
            File fx = new File(f);
            try (FileOutputStream fos = new FileOutputStream(fx)) {
                fos.write(b);
            }
        }
    }
}
