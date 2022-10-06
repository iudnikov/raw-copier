package ru.yudnikov;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Slf4j
@Value
@Builder
public class RawMover {

    Path jpgSource;
    Path rawSource;

    Path rawTarget;

    public void run() throws Exception {
        try (Stream<Path> pathStream = Files.walk(jpgSource)) {
            pathStream
                    .filter(f -> !Files.isDirectory(f))
                    .forEach(f -> handle(f));
        }
    }

    private void handle(Path f) {
        try {
            final File currentFile = f.toFile();
            log.info("file would be handled: {}", currentFile);

            String s = f.getFileName().toString();
            String name = s.split("\\.")[0];
            String raw = name + ".ARW";
            String dop = name + ".ARW.dop";


            final File fromRawFile = rawSource.resolve(name + ".ARW").toFile();
            final File fromDopFile = rawSource.resolve(name + ".ARW.dop").toFile();
            final File toRawFile = rawTarget.resolve(name + ".ARW").toFile();
            final File toDopFile = rawTarget.resolve(name + ".ARW.dop").toFile();
            //log.debug("moving to: {}", toRawFile);
            if (fromRawFile.exists()) {
                FileUtils.moveFile(fromRawFile, toRawFile);
            }
            if (fromDopFile.exists()) {
                FileUtils.moveFile(fromDopFile, toDopFile);
            }
        } catch (Exception e) {
            log.error("file can't be handled: {}", f);
        }

    }
}
