package sharedkernel.infrastructure.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import sharedkernel.domain.exceptions.GuardException;

class StartupBannerTest {

    @Test
    void shouldRenderOneRowPerFontLine() {
        var rows = AsciiFont.render("AT");

        assertThat(rows).hasSize(AsciiFont.HEIGHT);
        assertThat(rows.getFirst()).isEqualTo(" ###  #####");
    }

    @Test
    void shouldKeepEveryGlyphRowTheSameWidth() {
        for (var character = 'A'; character <= 'Z'; character++) {
            assertThat(AsciiFont.glyphFor(character))
                .as("glyph for %s", character)
                .hasSize(AsciiFont.HEIGHT)
                .allMatch(row -> row.length() == AsciiFont.WIDTH);
        }
    }

    @Test
    void shouldLeaveBlankSpaceForCharactersOutsideTheAlphabet() {
        assertThat(AsciiFont.render("A1")).allMatch(row -> row.endsWith("     "));
    }

    @Test
    void shouldRenderTheNameRegardlessOfCase() {
        assertThat(AsciiFont.render("at")).isEqualTo(AsciiFont.render("AT"));
    }

    @Test
    void shouldListEveryEntryWhenRendered() {
        var banner = StartupBanner
            .named("AGENDA")
            .withoutColor()
            .with("JDK", "25.0.1")
            .with("Port", "8080")
            .with("PID", "4242")
            .render();

        assertThat(banner).contains("JDK   25.0.1").contains("Port  8080").contains("PID   4242");
    }

    @Test
    void shouldNotEmitEscapeSequencesWhenColorIsDisabled() {
        var banner = StartupBanner.named("AGENDA").withoutColor().with("Port", "8080").render();

        assertThat(banner).doesNotContain(StartupBanner.ESC);
    }

    @Test
    void shouldEmitTheChosenColorWhenColorIsEnabled() {
        var banner = StartupBanner.named("A").withColor(true).colored(StartupBanner.DEEP_NAVY).render();

        assertThat(banner).contains(StartupBanner.DEEP_NAVY).contains(StartupBanner.RESET);
    }

    @Test
    void shouldReportTheRunningJdkAndProcess() {
        assertThat(StartupBanner.jdkVersion()).isNotBlank();
        assertThat(StartupBanner.processId()).containsOnlyDigits();
    }

    @Test
    void shouldFrameTheBannerBetweenTwoRules() {
        var banner = StartupBanner.named("A").withoutColor().with("Port", "8080").render();

        var rows = banner.split(Pattern.quote(System.lineSeparator()), -1);
        var content = Arrays.stream(rows).filter(row -> !row.isEmpty()).toList();

        assertThat(content.getFirst()).matches("=+");
        assertThat(content.getLast()).matches("=+");
        assertThat(content.getFirst()).isEqualTo(content.getLast());
    }

    @Test
    void shouldMakeTheRuleAsWideAsTheWidestRow() {
        var banner = StartupBanner
            .named("A")
            .withoutColor()
            .with("API", "http://localhost:8080/appointments")
            .render();

        var rows = banner.split(Pattern.quote(System.lineSeparator()), -1);
        var widest = Arrays.stream(rows).mapToInt(String::length).max().orElseThrow();

        assertThat(rows[1]).hasSize(widest);
    }

    @Test
    void shouldRejectABlankName() {
        assertThatThrownBy(() -> StartupBanner.named(" ")).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldKeepALiteralWordmarkExactlyAsGiven() {
        var art = String.join("\n", "  /\\  ", " /  \\ ", "/____\\");

        var banner = StartupBanner.showing(art).withoutColor().render();

        assertThat(banner).contains("  /\\  " + System.lineSeparator());
        assertThat(banner).contains(" /  \\ " + System.lineSeparator());
        assertThat(banner).contains("/____\\" + System.lineSeparator());
    }

    @Test
    void shouldNotLeaveCarriageReturnsInsideTheWordmarkRows() {
        var banner = StartupBanner.showing("/\\\r\n\\/").withoutColor().render();

        var rows = banner.split(Pattern.quote(System.lineSeparator()), -1);

        assertThat(rows).contains("/\\", "\\/");
    }

    @Test
    void shouldRejectABlankWordmark() {
        assertThatThrownBy(() -> StartupBanner.showing(" ")).isInstanceOf(GuardException.class);
    }
}
