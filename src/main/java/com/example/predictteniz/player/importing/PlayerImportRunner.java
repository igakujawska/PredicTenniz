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
@Profile("player-import")
public class PlayerImportRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PlayerImportRunner.class);
    private final PlayerCsvReader reader;
    private final PlayerImportService importer;
    private final ConfigurableApplicationContext context;
    private final Path path;
    private final String source;
    private final String expectedSha256;

    public PlayerImportRunner(PlayerCsvReader reader, PlayerImportService importer,
                              ConfigurableApplicationContext context,
                              @Value("${tennis.import.players.path}") Path path,
                              @Value("${tennis.import.players.source:SACKMANN_ATP}") String source,
                              @Value("${tennis.import.players.sha256:}") String expectedSha256) {
        this.reader = reader;
        this.importer = importer;
        this.context = context;
        this.path = path;
        this.source = source;
        this.expectedSha256 = expectedSha256;
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
        if (!expectedSha256.isBlank() && !sha256.equalsIgnoreCase(expectedSha256))
            throw new IllegalArgumentException("CSV checksum mismatch; import was not started.");
        PlayerCsvReader.ParseResult parsed;
        try (var input = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            parsed = reader.read(input);
        }
        if (parsed.players().isEmpty()) throw new IllegalArgumentException("CSV contains no valid player records.");
        var summary = importer.importPlayers(parsed.players(), source);
        log.info("CSV_IMPORT source={} sha256={} accepted={} rejected={} created={} alreadyKnown={}",
            source, sha256, parsed.players().size(), parsed.rejected().size(), summary.created(), summary.alreadyKnown());
        parsed.rejected().stream().limit(10).forEach(error ->
            log.warn("CSV_REJECTED record={} reason={}", error.recordNumber(), error.reason()));
        SpringApplication.exit(context);
    }
}
