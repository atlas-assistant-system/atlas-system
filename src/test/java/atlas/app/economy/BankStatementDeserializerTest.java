package atlas.app.economy;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.economy.commands.recordmovement.RecordMovementCommand;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BankStatementDeserializerTest {

    private static final String HEADER = ";Numero de cuenta;Oficina;Divisa;F. Operación;F. Valor;Ingreso (+);"
        + "Gasto (-);Saldo (+);Saldo (-);Concepto común;Concepto propio;Referencia 1;Referencia 2;"
        + "Concepto complementario 1;Concepto complementario 2;Concepto complementario 3;"
        + "Concepto complementario 4;Concepto complementario 5;Concepto complementario 6;"
        + "Concepto complementario 7;Concepto complementario 8;Concepto complementario 9;"
        + "Concepto complementario 10;";

    private static final String CARD_PURCHASE = ";2100 7101 70 1300033631;9736;EUR;17/08/2026;17/08/2026;;53,54;"
        + "24279,59;;12;040;000000000000;4466455019127427;      ESTACION BP LAS V     ;;;;;;;;"
        + "04000022SIO      COMPRA CON TARJETA      ;;";

    private static final String TRANSFER = ";2100 7101 70 1300033631;9792;EUR;19/08/2026;19/08/2026;17,10;;"
        + "24258,69;;04;002;000000000000;385219770340;;;;;   Bizum de Eduardo   ;;;;Carlos Alberto;;";

    @Test
    void shouldReadAnExpenseFromTheStatement(@TempDir Path directory) throws IOException {
        var commands = deserialize(directory, HEADER, CARD_PURCHASE);

        assertThat(commands).singleElement().satisfies(command -> {
            assertThat(command.kind()).isEqualTo(MovementKind.EXPENSE);
            assertThat(command.amount()).isEqualByComparingTo(new BigDecimal("53.54"));
            assertThat(command.category()).isEqualTo(Category.TRANSPORT);
            assertThat(command.note()).isEqualTo("ESTACION BP LAS V");
            assertThat(command.occurredOn()).isEqualTo(LocalDate.of(2026, 8, 17));
        });
    }

    @Test
    void shouldReadAnIncomeFromTheStatement(@TempDir Path directory) throws IOException {
        var commands = deserialize(directory, HEADER, TRANSFER);

        assertThat(commands).singleElement().satisfies(command -> {
            assertThat(command.kind()).isEqualTo(MovementKind.INCOME);
            assertThat(command.amount()).isEqualByComparingTo(new BigDecimal("17.10"));
            assertThat(command.category()).isEqualTo(Category.INCOME);
            assertThat(command.note()).isEqualTo("Bizum de Eduardo Carlos Alberto");
        });
    }

    @Test
    void shouldSkipTheNoiseAroundTheRows(@TempDir Path directory) throws IOException {
        var commands = deserialize(
            directory, ";MOVIMIENTOS DESDE : 01/01/2026 HASTA: 21/08/2026;;;", "", HEADER, CARD_PURCHASE, ";;;;;;");

        assertThat(commands).hasSize(1);
    }

    @Test
    void shouldLeaveUnknownShopsToBeRecategorizedByHand(@TempDir Path directory) throws IOException {
        var unknown = CARD_PURCHASE.replace("ESTACION BP LAS V", "YE HONGLIAN      ");

        assertThat(deserialize(directory, HEADER, unknown))
            .singleElement()
            .extracting(RecordMovementCommand::category)
            .isEqualTo(Category.OTHER);
    }

    @Test
    void shouldReadAmountsWrittenByAnEnglishExcelToo() {
        assertThat(BankStatementDeserializer.amountOf("1234.56")).isEqualByComparingTo(new BigDecimal("1234.56"));
        assertThat(BankStatementDeserializer.amountOf("1.234,56")).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    void shouldComplainWhenTheFileIsNotAStatement(@TempDir Path directory) throws IOException {
        var csv = write(directory, "fecha;importe", "01/01/2026;10,00");

        assertThat(catchThrowable(csv)).isInstanceOf(IllegalArgumentException.class);
    }

    private static Throwable catchThrowable(Path csv) {
        try {
            BankStatementDeserializer.deserialize(csv);

            return null;
        } catch (RuntimeException | IOException e) {
            return e;
        }
    }

    private static List<RecordMovementCommand> deserialize(Path directory, String... lines) throws IOException {
        return BankStatementDeserializer.deserialize(write(directory, lines));
    }

    private static Path write(Path directory, String... lines) throws IOException {
        var csv = directory.resolve("extracto.csv");
        Files.write(csv, List.of(lines), StandardCharsets.UTF_8);

        return csv;
    }
}
