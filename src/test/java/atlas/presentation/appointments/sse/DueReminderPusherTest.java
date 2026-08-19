package atlas.presentation.appointments.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import atlas.application.appointments.dto.DueReminderDto;
import atlas.application.sharedkernel.cqrs.QueryBus;
import atlas.domain.sharedkernel.results.Error;
import atlas.domain.sharedkernel.results.Result;
import atlas.presentation.sharedkernel.sse.SseHub;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DueReminderPusherTest {

    private static final Instant NOW = Instant.parse("2026-08-17T10:50:00Z");

    private final QueryBus queries = mock(QueryBus.class);
    private final SseHub hub = new SseHub();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final DueReminderPusher pusher =
        new DueReminderPusher(queries, hub, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void shouldPushADueReminderOnlyOnce() {
        hub.register(output);
        when(queries.dispatch(any())).thenReturn(Result.success(List.of(dueReminder())));

        pusher.pushDueReminders();
        pusher.pushDueReminders();

        var wire = output.toString(StandardCharsets.UTF_8);
        assertThat(wire).containsOnlyOnce("event: reminderDue");
        assertThat(wire).contains("\"appointmentId\":\"A00000001\"");
    }

    @Test
    void shouldPushNothingWhenTheQueryFails() {
        hub.register(output);
        when(queries.dispatch(any())).thenReturn(Result.failure(Error.unexpected("X", "boom")));

        pusher.pushDueReminders();

        assertThat(output.toString(StandardCharsets.UTF_8)).doesNotContain("reminderDue");
    }

    private static DueReminderDto dueReminder() {
        return new DueReminderDto(
            "A00000001", new UUID(0, 1).toString(), "Dentista", LocalDateTime.of(2026, 8, 17, 11, 0), 15);
    }
}
