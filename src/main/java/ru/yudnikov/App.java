package ru.yudnikov;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class App {

    private static Previewer previewer(Config conf) {
        String source = conf.getString("previewer.source");
        String target = conf.getString("previewer.target");
        return Previewer.builder()
                .source(Path.of(source))
                .target(Path.of(target))
                .build();
    }
    private static Copier copier(Config conf) {
        String source = conf.getString("copier.source");
        String target = conf.getString("copier.target");
        List<String> rawFormats = conf.getList("copier.rawFormats").stream()
                .map(x -> (String) x.unwrapped())
                .collect(Collectors.toList());
        List<String> jpgFormats = conf.getList("copier.jpgFormats").stream()
                .map(x -> (String) x.unwrapped())
                .collect(Collectors.toList());
        int lastDays = conf.getInt("copier.lastDays");
        return Copier.builder()
                .source(Path.of(source))
                .target(Path.of(target))
                .rawFormats(rawFormats)
                .jpgFormats(jpgFormats)
                .lastDays(lastDays)
                .build();
    }

    private static RawMover rawMover(Config conf) {
        String jpgSource = conf.getString("rawMover.jpgSource");
        String rawSource = conf.getString("rawMover.rawSource");
        String rawTarget = conf.getString("rawMover.rawTarget");
        return RawMover.builder()
                .jpgSource(Path.of(jpgSource))
                .rawSource(Path.of(rawSource))
                .rawTarget(Path.of(rawTarget))
                .build();
    }

    public static void main(String[] args) {
        try {
            Config conf = ConfigFactory.load();
            //rawMover(conf).run();
            copier(conf).run();
            //previewer(conf).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
