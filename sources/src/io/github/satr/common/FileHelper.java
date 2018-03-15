package io.github.satr.common;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public class FileHelper {
    public static int getZipContentSize(final File file) {
        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(file);
            return zipfile.size();
        } catch (IOException e) {
            return -1;
        } finally {
            try {
                if (zipfile != null) {
                    zipfile.close();
                }
            } catch (IOException e) {
            }
        }
    }
}
