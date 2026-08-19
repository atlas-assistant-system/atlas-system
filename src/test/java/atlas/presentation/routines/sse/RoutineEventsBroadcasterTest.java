package atlas.presentation.routines.sse;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.events.DayClearedEvent;
import atlas.domain.routines.events.ProgressLoggedEvent;
import atlas.domain.routines.events.RoutineArchivedEvent;
import atlas.domain.routines.events.RoutineDefinedEvent;
import atlas.domain.routines.events.RoutineDeletedEvent;
import atlas.domain.routines.events.RoutineScheduleChangedEvent;
import atlas.domain.routines.vos.RoutineName;
import atlas.domain.routines.vos.Schedule;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sharedkernel.application.events.SimpleDomainEventPublisher;
import sharedkernel.presentation.sse.SseHub;

class RoutineEventsBroadcasterTest {

    private static final Instant NOW = Instant.parse("2026-02-14T10:00:00Z");
    private static final RoutineId ID = RoutineId.of(1);
    private static final LocalDate DAY = LocalDate.of(2026, 2, 9);

    private SimpleDomainEventPublisher events;
    private SseHub hub;
    private ByteArrayOutputStream client;

    @BeforeEach
    void subscribe() {
        events = new SimpleDomainEventPublisher();
        hub = new SseHub();
        client = new ByteArrayOutputStream();
        hub.register(client);
        RoutineEventsBroadcaster.subscribeAll(events, hub);
    }

    @Test
    void shouldBroadcastTheDefinedRoutine() {
        events.publish(new RoutineDefinedEvent(ID, RoutineName.create("Correr").value(), NOW));

        assertThat(sent()).contains("event: routineDefined").contains("R00000001").contains("Correr");
    }

    @Test
    void shouldBroadcastTheNewPeriodWhenTheScheduleChanges() {
        events.publish(new RoutineScheduleChangedEvent(
            ID, Schedule.over(RecurrencePeriod.MONTH).value(), NOW));

        assertThat(sent()).contains("event: routineScheduleChanged").contains("MONTH");
    }

    @Test
    void shouldBroadcastTheLoggedAmountAndItsDay() {
        events.publish(new ProgressLoggedEvent(ID, DAY, new BigDecimal("1.5"), NOW));

        assertThat(sent())
            .contains("event: progressLogged")
            .contains("2026-02-09")
            .contains("1.5");
    }

    @Test
    void shouldBroadcastTheClearedDay() {
        events.publish(new DayClearedEvent(ID, DAY, NOW));

        assertThat(sent()).contains("event: dayCleared").contains("2026-02-09");
    }

    @Test
    void shouldBroadcastTheRoutineIdOfTheSimpleEvents() {
        events.publish(new RoutineArchivedEvent(ID, NOW));
        events.publish(new RoutineDeletedEvent(ID, NOW));

        assertThat(sent()).contains("event: routineArchived").contains("event: routineDeleted");
    }

    private String sent() {
        return client.toString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
