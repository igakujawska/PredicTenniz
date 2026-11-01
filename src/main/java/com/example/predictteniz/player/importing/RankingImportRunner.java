package com.example.predictteniz.player.importing;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("ranking-import")
public class RankingImportRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(RankingImportRunner.class);
    private final RankingImportService importer;
    private final ConfigurableApplicationContext context;
    private final Path path;
    private final String checksum;

    public RankingImportRunner(RankingImportService importer, ConfigurableApplicationContext context,
            @Value("${tennis.import.rankings.path}") Path path,
            @Value("${tennis.import.rankings.sha256}") String checksum) {
        this.importer = importer;
        this.context = context;
        this.path = path;
        this.checksum = checksum;
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
        if (!actual.equalsIgnoreCase(checksum)) throw new IllegalArgumentException("Ranking CSV checksum mismatch.");
        try (var input = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            var result = importer.importLatest(input);
            log.info("RANKING_IMPORT date={} created={} alreadyKnown={} unknownPlayers={} sha256={}",
                result.date(), result.created(), result.alreadyKnown(), result.unknownPlayers(), actual);
        }
        SpringApplication.exit(context);
    }
}
