package atlas.app.economy;

import atlas.application.economy.commands.recordmovement.RecordMovementCommand;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import atlas.domain.economy.vos.MovementNote;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class BankStatementDeserializer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final Pattern OPERATION_CODE = Pattern.compile("\\d{8}[A-Z]{3}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final String CARD_PURCHASE = "COMPRA CON TARJETA";

    private static final String DATE_COLUMN = "f. operacion";
    private static final String INCOME_COLUMN = "ingreso (+)";
    private static final String EXPENSE_COLUMN = "gasto (-)";
    private static final String CONCEPT_COLUMN = "concepto complementario ";

    private static final Map<Category, List<String>> KEYWORDS = Map.of(
        Category.TRANSPORT, List.of(
            "ESTACION BP", "REPSOL", "CEPSA", "DISA", "RYANAIR", "IBERIA", "RENFE", "OASA", "ATAC",
            "TAXI", "AUTGC", "GLOBAL SALCAI", "FRED.OLSEN", "AEROPUERTO", "AIRPORT", "PARKING",
            "EMPRESA MUNICIPAL", "VIA ULISES", "GIULIO AGRICOLA"),
        Category.HEALTH, List.of("FARMACIA", "CLINICA", "DENTAL", "HOSPITAL", "OPTICA"),
        Category.FOOD, List.of(
            "MC DONALD", "MCDONALD", "SUPERMERCADO", "SPAR", "CARREFOUR", "MERCADONA", "HIPERDINO",
            "LIDL", "ALCAMPO", "GLOVO", "JUST EAT", "CHURRASCO", "CASA RICARDO", "CAFE", "GELATO",
            "LOUKOUMADES", "FSV GRAN CANARIA", "KIOSKO", "PANADERIA", "RESTAURANT", "PIZZ", "FOOD",
            "SHOP&GO", "SHOP & GO", "SHOPNGO", "VENDING", "SELECTA", "TABACCHI", "WINEST", "BAR "),
        Category.LEISURE, List.of(
            "STEAMGAMES", "BLIZZARD", "CRUNCHYROLL", "NETFLIX", "SPOTIFY", "SIAM PARK", "MICROSOFT*STORE",
            "BOOKING", "AIRBNB", "HOTEL", "RESORT", "CINE", "MUSEO", "BIBLIOTECA"),
        Category.SHOPPING, List.of(
            "BAZAAR", "STORE", "AMAZON", "ZARA", "PRIMARK", "DECATHLON", "MEDIA MARKT", "EL CORTE INGLES"),
        Category.HOME, List.of(
            "ENDESA", "IBERDROLA", "NATURGY", "MOVISTAR", "VODAFONE", "ORANGE", "ALQUILER", "COMUNIDAD",
            "AGUAS"));

    private BankStatementDeserializer() {}

    public static List<RecordMovementCommand> deserialize(Path csv) throws IOException {
        var lines = Files.readAllLines(csv, charsetOf(csv));
        var header = headerIndexOf(lines);
        var separator = separatorOf(lines.get(header));
        var columns = columnsOf(lines.get(header), separator);

        var commands = new ArrayList<RecordMovementCommand>();
        for (var line : lines.subList(header + 1, lines.size())) {
            var cells = split(line, separator);
            var command = movementOf(cells, columns);
            if (command != null) {
                commands.add(command);
            }
        }

        return List.copyOf(commands);
    }

    private static RecordMovementCommand movementOf(List<String> cells, Map<String, Integer> columns) {
        var date = cell(cells, columns.get(DATE_COLUMN));
        if (date.isBlank()) {
            return null;
        }

        var income = cell(cells, columns.get(INCOME_COLUMN));
        var kind = income.isBlank() ? MovementKind.EXPENSE : MovementKind.INCOME;
        var amount = amountOf(income.isBlank() ? cell(cells, columns.get(EXPENSE_COLUMN)) : income);
        var note = noteOf(cells, columns);

        return new RecordMovementCommand(
            kind, amount, categoryOf(kind, note), note, LocalDate.parse(date.trim(), DATE));
    }

    static Category categoryOf(MovementKind kind, String note) {
        if (kind == MovementKind.INCOME) {
            return Category.INCOME;
        }

        var upper = note.toUpperCase(Locale.ROOT);
        for (var category : List.of(
            Category.TRANSPORT, Category.HEALTH, Category.FOOD,
            Category.LEISURE, Category.SHOPPING, Category.HOME)) {

            if (KEYWORDS.get(category).stream().anyMatch(upper::contains)) {
                return category;
            }
        }

        return Category.OTHER;
    }

    private static String noteOf(List<String> cells, Map<String, Integer> columns) {
        var text = new StringBuilder();
        for (var concept = 1; concept <= 10; concept++) {
            var index = columns.get(CONCEPT_COLUMN + concept);
            if (index == null) {
                continue;
            }

            var part = OPERATION_CODE.matcher(cell(cells, index)).replaceAll("").replace(CARD_PURCHASE, "");
            if (!part.isBlank()) {
                text.append(part).append(' ');
            }
        }

        var note = WHITESPACE.matcher(text).replaceAll(" ").trim();
        if (note.isBlank()) {
            return "Movimiento bancario";
        }

        return note.length() > MovementNote.MAX_LENGTH ? note.substring(0, MovementNote.MAX_LENGTH).trim() : note;
    }

    static BigDecimal amountOf(String value) {
        var cleaned = WHITESPACE.matcher(value).replaceAll("").replace(" ", "");

        return new BigDecimal(cleaned.contains(",") ? cleaned.replace(".", "").replace(',', '.') : cleaned);
    }

    private static int headerIndexOf(List<String> lines) {
        for (var index = 0; index < lines.size(); index++) {
            if (normalize(lines.get(index)).contains(DATE_COLUMN)) {
                return index;
            }
        }

        throw new IllegalArgumentException(
            "El CSV no tiene la cabecera del extracto: falta la columna 'F. Operacion'.");
    }

    private static Map<String, Integer> columnsOf(String header, char separator) {
        var cells = split(header, separator);
        var columns = new HashMap<String, Integer>();
        for (var index = 0; index < cells.size(); index++) {
            columns.put(normalize(cells.get(index)), index);
        }

        return Map.copyOf(columns);
    }

    private static char separatorOf(String header) {
        return header.chars().filter(character -> character == ';').count() >= header.chars()
            .filter(character -> character == ',').count() ? ';' : ',';
    }

    private static List<String> split(String line, char separator) {
        var cells = new ArrayList<String>();
        var cell = new StringBuilder();
        var quoted = false;

        for (var character : line.toCharArray()) {
            if (character == '"') {
                quoted = !quoted;
            } else if (character == separator && !quoted) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(character);
            }
        }
        cells.add(cell.toString());

        return cells;
    }

    private static String cell(List<String> cells, Integer index) {
        return index == null || index >= cells.size() ? "" : cells.get(index).trim();
    }

    private static String normalize(String value) {
        return WHITESPACE
            .matcher(Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", ""))
            .replaceAll(" ")
            .replace("\"", "")
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    private static Charset charsetOf(Path csv) throws IOException {
        try {
            Files.readString(csv, StandardCharsets.UTF_8);

            return StandardCharsets.UTF_8;
        } catch (MalformedInputException e) {
            return StandardCharsets.ISO_8859_1;
        }
    }
}
