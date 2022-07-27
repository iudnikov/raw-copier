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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Value
@Builder
public class FileCopier {
    Path source;
    Path target;
    int days;
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
        File dir = source.toFile();
        if (!dir.exists()) {
            log.warn("source dir {} does not exists", dir);
            return;
        }

        if (!target.toFile().exists()) {
            log.debug("target would be created");
            createDir(target.toFile());
        }
        Files.walk(dir.toPath())
                .filter(f -> Files.isRegularFile(f) && f.getFileName().toString().endsWith(".ARW"))
                .forEach(f -> {
                    try {
                        final File currentFile = f.toFile();
                        final Extractor extractor = Extractor.builder().file(currentFile).build();
                        final FileData data = extractor.readData();

                        final LocalDateTime copySince = LocalDateTime.now().minusDays(days);

                        if (copySince.isAfter(data.getDateTime())) {
                            log.debug("file would be skipped: {}", data.dateTime);
                        } else {
                            final int year = data.dateTime.getYear();
                            final File yearSubDir = target.resolve(String.format("%s", year)).toFile();
                            if (!yearSubDir.exists()) {
                                createDir(yearSubDir);
                            }
                            final File dateSubDir = yearSubDir.toPath().resolve(data.dateTime.toLocalDate().toString()).toFile();
                            if (!dateSubDir.exists()) {
                                createDir(dateSubDir);
                            }
                            final String toFileName = getToFileName(currentFile, data);
                            final File toFile = dateSubDir.toPath().resolve(toFileName).toFile();
                            log.debug("copying to: {}", toFile);
                            if (!toFile.exists()) {
                                FileUtils.copyFile(currentFile, toFile);
                            } else {
                                log.warn("file exists and would be skipped: {}", toFile);
                            }
                        }
                    } catch (Exception e) {
                        log.error("can't copy file: {}", f, e);
                    }
                });
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

    private void createDir(File dirToCreate) throws Exception {
        log.debug("directory would be created: {}", dirToCreate);
        final boolean mkdir = dirToCreate.mkdir();
        if (!mkdir) {
            throw new Exception(String.format("can't create directory: %s", dirToCreate));
        }
    }

    static FileCopier of(String sourceDir, String targetDir, int days) {
        return FileCopier.builder()
                .source(Path.of(sourceDir))
                .target(Path.of(targetDir))
                .days(days)
                .build();
    }
}
