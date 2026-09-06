package atlas.infrastructure.sharedkernel.console;

import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.guards.StringGuard;
import atlas.infrastructure.sharedkernel.logging.LogEntryRenderers;
import java.util.ArrayList;
import java.util.List;

public final class StartupBanner {

    public static final String ESC = String.valueOf((char) 27);
    public static final String RESET = ESC + "[0m";
    public static final String DIM = ESC + "[90m";
    public static final String DEEP_NAVY = rgb(11, 31, 58);
    public static final String PRIMARY_BLUE = rgb(36, 107, 253);
    public static final String CYAN = rgb(54, 197, 240);
    public static final String RULE = "=";

    private static final String INDENT = "  ";
    private static final String GAP = "  ";

    private final List<String> wordmark;
    private final List<BannerEntry> entries = new ArrayList<>();

    private String color = PRIMARY_BLUE;
    private boolean colored = LogEntryRenderers.colorIsSupported();

    private StartupBanner(List<String> wordmark) {
        this.wordmark = wordmark;
    }

    public static StartupBanner named(String name) {
        return new StartupBanner(AsciiFont.render(StringGuard.notBlank(name, "name")));
    }

    public static StartupBanner showing(String wordmark) {
        StringGuard.notBlank(wordmark, "wordmark");

        return new StartupBanner(List.of(wordmark.replace("\r", "").split("\n", -1)));
    }

    public static String rgb(int red, int green, int blue) {
        return ESC + "[38;2;" + red + ";" + green + ";" + blue + "m";
    }

    public static String jdkVersion() {
        return Runtime.version().toString();
    }

    public static String processId() {
        return String.valueOf(ProcessHandle.current().pid());
    }

    public StartupBanner colored(String ansiColor) {
        this.color = ObjectGuard.notNull(ansiColor, "ansiColor");

        return this;
    }

    public StartupBanner withColor(boolean enabled) {
        this.colored = enabled;

        return this;
    }

    public StartupBanner withoutColor() {
        return withColor(false);
    }

    public StartupBanner with(String label, String value) {
        StringGuard.notBlank(label, "label");
        ObjectGuard.notNull(value, "value");

        entries.add(new BannerEntry(label, value));

        return this;
    }

    public String render() {
        var labelWidth = entries.stream().mapToInt(entry -> entry.label().length()).max().orElse(0);
        var rule = RULE.repeat(widthOf(labelWidth));
        var banner = new StringBuilder(System.lineSeparator());

        banner.append(paint(rule, color)).append(System.lineSeparator());

        for (var row : wordmark) {
            banner.append(paint(row, color)).append(System.lineSeparator());
        }

        if (!entries.isEmpty()) {
            banner.append(System.lineSeparator());
            for (var entry : entries) {
                banner.append(INDENT).append(paint(pad(entry.label(), labelWidth), DIM));
                banner.append(GAP).append(entry.value()).append(System.lineSeparator());
            }
            banner.append(System.lineSeparator());
        }

        banner.append(paint(rule, color)).append(System.lineSeparator());

        return banner.append(System.lineSeparator()).toString();
    }

    private int widthOf(int labelWidth) {
        var widestWordmark = wordmark.stream().mapToInt(String::length).max().orElse(0);
        var widestEntry = entries
            .stream()
            .mapToInt(entry -> INDENT.length() + labelWidth + GAP.length() + entry.value().length())
            .max()
            .orElse(0);

        return Math.max(widestWordmark, widestEntry);
    }

    private String paint(String text, String ansiColor) {
        if (!colored) {
            return text;
        }

        return ansiColor + text + RESET;
    }

    private static String pad(String label, int width) {
        return label + " ".repeat(width - label.length());
    }
}
