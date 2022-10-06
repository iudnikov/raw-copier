package ru.yudnikov;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.yudnikov.Common.createDirIfNotExists;

@Slf4j
@Value
@Builder
public class Copier {
    Path source;
    Path target;
    int lastDays;
    List<String> rawFormats;
    List<String> jpgFormats;

    @Value
    @Builder
    static class FileData {
        LocalDateTime dateTime;
        String cameraModel;
    }

    @Value
    @Builder(toBuilder = true)
    static class Extractor {
        File file;

        public FileData readData() throws Exception {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            final FileData.FileDataBuilder fileDataBuilder = FileData.builder();
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    if ("Date/Time".equals(tag.getTagName())) {
                        final String value = tag.getDescription();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
                        fileDataBuilder.dateTime(LocalDateTime.parse(value, formatter));
                    } else if ("Model".equals(tag.getTagName())) {
                        fileDataBuilder.cameraModel(tag.getDescription());
                    }
                }
            }
            return fileDataBuilder.build();
        }
    }

    public void run() throws Exception {
        log.info("running with source: {}, target: {}", source, target);
        File dir = source.toFile();
        if (!dir.exists()) {
            log.warn("source dir: {} does not exists", dir);
            return;
        }

        createDirIfNotExists(target.toFile());

        List<String> formats = Stream.concat(rawFormats.stream(), jpgFormats.stream()).collect(Collectors.toList());

        try (Stream<Path> pathStream = Files.walk(dir.toPath())) {
            pathStream.filter(f -> {
                        boolean isHidden;
                        try {
                            isHidden = Files.isHidden(f);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        boolean matches = Files.isRegularFile(f)
                                && !isHidden
                                && (formats.stream().anyMatch(format -> f.getFileName().toString().toLowerCase().endsWith(format)));
                        return matches;
                    })
                    .forEach(f -> handle(f));
        }


    }

    private void handle(Path f) {
        try {
            final File currentFile = f.toFile();
            log.info("file would be handled: {}", currentFile);

            if (!currentFile.exists()) {
                log.warn("file does not exists: {}", f);
                return;
            }
            if (currentFile.getName().startsWith(".")) {
                log.warn("hidden file would be skipped {}", currentFile.getName());
                return;
            }
            final Extractor extractor = Extractor.builder().file(currentFile).build();
            final FileData data = extractor.readData();

            final LocalDateTime startDate = LocalDateTime.now().minusDays(lastDays);

            if (startDate.isAfter(data.getDateTime())) {
                log.debug("file is too old: {}", data.dateTime);
            } else {
                final int year = data.dateTime.getYear();
                final File yearSubDir = target.resolve(String.format("%s", year)).toFile();
                createDirIfNotExists(yearSubDir);
                final File dateSubDir = yearSubDir.toPath().resolve(data.dateTime.toLocalDate().toString()).toFile();
                createDirIfNotExists(dateSubDir);
                final String toFileName = getToFileName(currentFile, data);
                final File toFile = dateSubDir.toPath().resolve(toFileName).toFile();
                log.debug("moving to: {}", toFile);
                if (!toFile.exists()) {
                    FileUtils.moveFile(currentFile, toFile);
                } else {
                    log.warn("destination file exists: {}", toFile);
                }
            }
        } catch (Exception e) {
            log.error("file can't be handled: {}", f);
        }

    }

    private static void moveDopFile(File currentFile, File dateSubDir, String toFileName) throws IOException {
        File dopFile = new File(currentFile.getAbsolutePath() + ".dop");
        if (dopFile.exists()) {
            final File toDopFile = dateSubDir.toPath().resolve(toFileName + ".dop").toFile();
            if (!toDopFile.exists()) {
                FileUtils.moveFile(dopFile, toDopFile);
            } else {
                log.warn("dop file exists and would be skipped: {}", toDopFile);
            }
        }
    }

    private String getToFileName(File currentFile, FileData data) {
        String toFileName = currentFile.getName();
        if (currentFile.getName().contains("_") && currentFile.getName().startsWith(data.cameraModel)) {
            String[] split = currentFile.getName().split("_");
            toFileName = split[1];
            log.debug("file would be renamed from {} to {}", currentFile.getName(), toFileName);
        }
        return toFileName;
    }

    static Copier of(String sourceDir, String targetDir, int days) {
        return Copier.builder()
                .source(Path.of(sourceDir))
                .target(Path.of(targetDir))
                .rawFormats(List.of("arw", "dng"))
                .jpgFormats(List.of("jpg", "jpeg"))
                .lastDays(days)
                .build();
    }
}
