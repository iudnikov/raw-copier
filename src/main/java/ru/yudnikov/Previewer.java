package ru.yudnikov;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static ru.yudnikov.Common.createDirIfNotExists;

@Slf4j
@Value
@Builder
public class Previewer {

    Path source;

    Path target;

    List<String> jpegFormats;

    public void run() throws Exception {
        log.info("copying previews from: {} to: {}", source, target);
        // get last segment
        String[] split = source.toString().split("/");
        String dir = split[split.length - 1];
        Path toDir = target.resolve(dir);
        createDirIfNotExists(toDir.toFile());

        try (Stream<Path> pathStream = Files.walk(source)) {
            pathStream
                    .filter(f -> Files.isRegularFile(f)
                            && isHidden(f)
                            && (jpegFormats.stream().anyMatch(format -> f.getFileName().toString().toLowerCase().endsWith(format)))
                    )
                    .forEach(f -> handle(f));
        }
    }

    private void handle(Path f) {
        try {

        } catch (Exception e) {
            log.error("can't handle file", e);
        }
    }

    private static boolean isHidden(Path f) {
        boolean isHidden;
        try {
            isHidden = Files.isHidden(f);
        } catch (IOException e) {
            log.warn("exception would be swallowed", e);
            return true;
        }
        return isHidden;
    }
}
