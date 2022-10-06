package ru.yudnikov;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class Common {

    public static void createDirIfNotExists(File dir) throws Exception {
        if (dir.exists()) {
            log.debug("dir already exists: {}", dir);
            return;
        }
        log.debug("directory would be created: {}", dir);
        final boolean mkdir = dir.mkdir();
        if (!mkdir) {
            throw new Exception(String.format("can't create directory: %s", dir));
        }
    }

}
