package sharedkernel.presentation.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import sharedkernel.domain.exceptions.GuardException;

class SseEventTest {

    @Test
    void shouldEndWithBlankLineWhenEncoded() {
        var frame = SseEvent.named("appointmentScheduled", "{\"id\":\"A00000007\"}").toWireFormat();

        assertThat(frame).isEqualTo("event: appointmentScheduled\ndata: {\"id\":\"A00000007\"}\n\n");
    }

    @Test
    void shouldIncludeIdWhenStamped() {
        var frame = SseEvent.named("ping", "1").withId("42").toWireFormat();

        assertThat(frame).isEqualTo("id: 42\nevent: ping\ndata: 1\n\n");
    }

    @Test
    void shouldRepeatDataPrefixOnEveryLineWhenDataIsMultiline() {
        var frame = SseEvent.named("report", "first\nsecond\nthird").toWireFormat();

        assertThat(frame).isEqualTo("event: report\ndata: first\ndata: second\ndata: third\n\n");
    }

    @Test
    void shouldNormalizeWindowsLineBreaksWhenDataHasThem() {
        var frame = SseEvent.named("report", "first\r\nsecond").toWireFormat();

        assertThat(frame).isEqualTo("event: report\ndata: first\ndata: second\n\n");
        assertThat(frame).doesNotContain("\r");
    }

    @Test
    void shouldEmitEmptyDataLineWhenDataIsEmpty() {
        var frame = SseEvent.named("tick", "").toWireFormat();

        assertThat(frame).isEqualTo("event: tick\ndata: \n\n");
    }

    @Test
    void shouldRejectLineBreakInNameWhenItWouldForgeAFrame() {
        assertThatThrownBy(() -> SseEvent.named("evil\ndata: injected", "x"))
            .isInstanceOf(GuardException.class)
            .hasMessageContaining("cannot contain line breaks");
    }

    @Test
    void shouldRejectLineBreakInIdWhenItWouldForgeAFrame() {
        assertThatThrownBy(() -> SseEvent.named("ok", "x").withId("1\nevent: injected"))
            .isInstanceOf(GuardException.class)
            .hasMessageContaining("cannot contain line breaks");
    }

    @Test
    void shouldRejectBlankName() {
        assertThatThrownBy(() -> SseEvent.named(" ", "x")).isInstanceOf(GuardException.class);
    }
}
