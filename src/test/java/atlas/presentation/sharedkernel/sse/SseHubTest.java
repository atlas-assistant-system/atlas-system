package atlas.presentation.sharedkernel.sse;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SseHubTest {

    private final SseHub hub = new SseHub();

    private static String textOf(ByteArrayOutputStream stream) {
        return stream.toString(StandardCharsets.UTF_8);
    }

    @Test
    void shouldReachEveryClientWhenBroadcasting() {
        var first = new ByteArrayOutputStream();
        var second = new ByteArrayOutputStream();
        hub.register(first);
        hub.register(second);

        var delivered = hub.broadcast(SseEvent.named("tick", "1"));

        assertThat(delivered).isEqualTo(2);
        assertThat(textOf(first)).contains("event: tick").contains("data: 1");
        assertThat(textOf(second)).contains("event: tick");
    }

    @Test
    void shouldStampIncreasingIdsWhenBroadcastingSeveralEvents() {
        var stream = new ByteArrayOutputStream();
        hub.register(stream);

        hub.broadcast(SseEvent.named("tick", "a"));
        hub.broadcast(SseEvent.named("tick", "b"));

        assertThat(textOf(stream)).contains("id: 1").contains("id: 2");
    }

    @Test
    void shouldDropClientWhenItsConnectionIsBroken() {
        var healthy = new ByteArrayOutputStream();
        hub.register(healthy);
        hub.register(new BrokenPipe());

        var delivered = hub.broadcast(SseEvent.named("tick", "1"));

        assertThat(delivered).isEqualTo(1);
        assertThat(hub.connectedCount()).isEqualTo(1);
    }

    @Test
    void shouldKeepServingRemainingClientsWhenOneDied() {
        var healthy = new ByteArrayOutputStream();
        hub.register(healthy);
        hub.register(new BrokenPipe());

        hub.broadcast(SseEvent.named("first", "1"));
        hub.broadcast(SseEvent.named("second", "2"));

        assertThat(textOf(healthy)).contains("event: first").contains("event: second");
    }

    @Test
    void shouldWriteCommentWhenSendingHeartbeat() {
        var stream = new ByteArrayOutputStream();
        hub.register(stream);

        var delivered = hub.sendHeartbeat();

        assertThat(delivered).isEqualTo(1);
        assertThat(textOf(stream)).isEqualTo(": ping\n\n");
    }

    @Test
    void shouldPruneDeadClientWhenHeartbeatFails() {
        hub.register(new BrokenPipe());

        hub.sendHeartbeat();

        assertThat(hub.connectedCount()).isZero();
    }

    @Test
    void shouldGiveEachClientItsOwnIdWhenRegistered() {
        var first = hub.register(new ByteArrayOutputStream());
        var second = hub.register(new ByteArrayOutputStream());

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(hub.connectedCount()).isEqualTo(2);
    }

    @Test
    void shouldCloseEveryClientWhenShuttingDown() {
        var client = hub.register(new ByteArrayOutputStream());

        hub.closeAll();

        assertThat(hub.connectedCount()).isZero();
        assertThat(client.isOpen()).isFalse();
    }

    @Test
    void shouldRequireTheStreamingHeadersWhenOpeningAResponse() {
        assertThat(SseHeaders.forStream())
            .containsEntry("Content-Type", "text/event-stream;charset=UTF-8")
            .containsEntry("Cache-Control", "no-cache, no-transform")
            .doesNotContainKey("Content-Length");
    }
}
