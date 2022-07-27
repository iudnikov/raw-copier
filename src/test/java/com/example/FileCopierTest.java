package com.example;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class FileCopierTest {

    static final String targetDir = "data/target";
    static final String sourceDir = "data/source/ILCE-6400/DCIM";
    static final int days = 120;

    @BeforeAll
    static void cleanUp() throws IOException {
        log.debug("target directory would be deleted");
        final File t = new File(targetDir);
        if (t.exists() && t.isDirectory()) {
            FileUtils.deleteDirectory(t);
        }
        FileUtils.forceMkdir(t);
    }

    @Test
    void testRun() throws Exception {
        FileCopier copier = FileCopier.of(sourceDir, targetDir, days);
        copier.run();
    }

}