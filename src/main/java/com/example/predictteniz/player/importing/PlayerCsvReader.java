package com.example.predictteniz.player.importing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class PlayerCsvReader {
    private static final Set<String> REQUIRED = Set.of("player_id", "name_first", "name_last", "dob", "ioc");

    public ParseResult read(Reader input) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        reader.mark(1);
        if (reader.read() != '\uFEFF') reader.reset();
        var format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).get();
        try (var parser = format.parse(reader)) {
            if (!parser.getHeaderMap().keySet().containsAll(REQUIRED))
                throw new IllegalArgumentException("CSV must contain player_id, name_first, name_last, dob and ioc.");
            List<PlayerRow> players = new ArrayList<>();
            List<RowError> rejected = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (CSVRecord record : parser.getRecords()) {
                try {
                    if (!record.isConsistent()) throw new IllegalArgumentException("Unexpected column count.");
                    String id = record.get("player_id").strip();
                    if (!id.matches("[1-9][0-9]*") || id.length() > 100)
                        throw new IllegalArgumentException("Invalid source player ID.");
                    String first = name(record.get("name_first"));
                    String last = name(record.get("name_last"));
                    String country = record.get("ioc").strip().toUpperCase(Locale.ROOT);
                    if (country.isEmpty()) country = null;
                    else if (!country.matches("[A-Z]{3}")) throw new IllegalArgumentException("Invalid country code.");
                    String dob = record.get("dob").strip();
                    LocalDate birth = dob.isEmpty() ? null : LocalDate.parse(dob, DateTimeFormatter.BASIC_ISO_DATE);
                    if (!seen.add(id)) throw new IllegalArgumentException("Duplicate source player ID.");
                    players.add(new PlayerRow(id, first, last, country, birth));
                } catch (IllegalArgumentException | DateTimeParseException error) {
                    rejected.add(new RowError(record.getRecordNumber(), error.getMessage()));
                }
            }
            return new ParseResult(List.copyOf(players), List.copyOf(rejected));
        } catch (UncheckedIOException error) {
            throw error.getCause();
        }
    }

    private String name(String value) {
        String name = value.strip();
        if (name.isEmpty() || name.length() > 100) throw new IllegalArgumentException("Name must have 1-100 characters.");
        return name;
    }

    public record PlayerRow(String externalId, String firstName, String lastName,
                            String countryCode, LocalDate birthDate) {}
    public record RowError(long recordNumber, String reason) {}
    public record ParseResult(List<PlayerRow> players, List<RowError> rejected) {}
}
