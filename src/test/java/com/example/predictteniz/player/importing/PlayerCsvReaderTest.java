package com.example.predictteniz.player.importing;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PlayerCsvReaderTest {
    private static final String HEADER = "player_id,name_first,name_last,dob,ioc\n";
    private final PlayerCsvReader reader = new PlayerCsvReader();

    @Test
    void handlesBomQuotedNamesAndExtraColumns() throws Exception {
        String csv = "\uFEFF" + """
            player_id,name_first,name_last,hand,dob,ioc,height,wikidata_id
            101,"Ana, Maria","O""Neil",R,20000115,POL,180,Q1
            """;
        var result = reader.read(new StringReader(csv));
        assertThat(result.rejected()).isEmpty();
        var player = result.players().getFirst();
        assertThat(player.firstName()).isEqualTo("Ana, Maria");
        assertThat(player.lastName()).isEqualTo("O\"Neil");
        assertThat(player.birthDate()).isEqualTo(LocalDate.of(2000, 1, 15));
    }

    @Test
    void missingOptionalDetailsRemainUnknown() throws Exception {
        var result = reader.read(new StringReader(HEADER + "101,Alex,Probe,,\n"));
        assertThat(result.players()).hasSize(1);
        assertThat(result.players().getFirst().countryCode()).isNull();
        assertThat(result.players().getFirst().birthDate()).isNull();
    }

    @Test
    void rejectsBadDataAndDuplicatesWithRecordNumbers() throws Exception {
        var result = reader.read(new StringReader(HEADER + """
            101,A,B,20240230,POL
            102,A,B,20000101,POL
            102,A,B,20000101,POL
            103,A,B,20000101,PL
            0,A,B,,POL
            104, ,B,,POL
            """));
        assertThat(result.players()).extracting(PlayerCsvReader.PlayerRow::externalId).containsExactly("102");
        assertThat(result.rejected()).extracting(PlayerCsvReader.RowError::recordNumber)
            .containsExactly(1L, 3L, 4L, 5L, 6L);
    }

    @Test
    void rejectsMissingHeadersBeforeImport() {
        assertThatThrownBy(() -> reader.read(new StringReader("id,name\n101,Alex\n")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void malformedCsvFailsInsteadOfReturningPartialData() {
        assertThatThrownBy(() -> reader.read(new StringReader(HEADER + "101,\"unterminated")))
            .isInstanceOf(IOException.class);
    }
}
