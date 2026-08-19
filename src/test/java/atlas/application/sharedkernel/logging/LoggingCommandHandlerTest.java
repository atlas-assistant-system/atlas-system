package atlas.application.sharedkernel.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.sharedkernel.results.Error;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoggingCommandHandlerTest {

    private static final String SENSITIVE = "resultado de la analitica del paciente";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-16T10:15:30Z"), ZoneOffset.UTC);

    private record ScheduleAppointment(String slot, String notes) implements Command<Result<String>> {}

    private record SummarisedCommand(String notes) implements Command<Result<String>>, LoggableSummary {

        @Override
        public String logSummary() {
            return "slot=2026-08-17T09:00";
        }
    }

    private record NoisyCommand() implements Command<Result<String>>, LoggableSummary {

        @Override
        public String logSummary() {
            return "first\nINFO: forged line\tend";
        }
    }

    private final RecordingLogger logger = new RecordingLogger();

    private <C extends Command<R>, R> LoggingCommandHandler<C, R> decorate(CommandHandler<C, R> inner) {
        return new LoggingCommandHandler<>(inner, logger, new PlainLogEntryRenderer(), CLOCK);
    }

    @Test
    void shouldLogNameAndSuccessWhenHandlerSucceeds() {
        var handler = decorate((ScheduleAppointment c) -> Result.success("ok"));

        handler.handle(new ScheduleAppointment("09:00", SENSITIVE));

        assertThat(logger.single().message())
            .contains("type=command")
            .contains("name=ScheduleAppointment")
            .contains("outcome=success");
        assertThat(logger.single().level()).isEqualTo(System.Logger.Level.INFO);
    }

    @Test
    void shouldNotLogCommandFieldsWhenCommandHasNoSummary() {
        var handler = decorate((ScheduleAppointment c) -> Result.success("ok"));

        handler.handle(new ScheduleAppointment("09:00", SENSITIVE));

        assertThat(logger.single().message()).doesNotContain(SENSITIVE);
        assertThat(logger.single().message()).doesNotContain("summary=");
    }

    @Test
    void shouldLogSummaryWhenCommandProvidesOne() {
        var handler = decorate((SummarisedCommand c) -> Result.success("ok"));

        handler.handle(new SummarisedCommand(SENSITIVE));

        assertThat(logger.single().message()).contains("summary=slot=2026-08-17T09:00");
        assertThat(logger.single().message()).doesNotContain(SENSITIVE);
    }

    @Test
    void shouldStripControlCharactersWhenSummaryContainsThem() {
        var handler = decorate((NoisyCommand c) -> Result.success("ok"));

        handler.handle(new NoisyCommand());

        assertThat(logger.single().message()).doesNotContain("\n");
        assertThat(logger.single().message()).contains("summary=first INFO: forged line end");
    }

    @Test
    void shouldLogErrorCodeWhenResultIsFailure() {
        var handler = decorate(
            (ScheduleAppointment c) -> Result.<String>failure(Error.conflict("APPOINTMENT_CANCELLED", "cancelled")));

        handler.handle(new ScheduleAppointment("09:00", SENSITIVE));

        assertThat(logger.single().message())
            .contains("outcome=failure")
            .contains("errorCode=APPOINTMENT_CANCELLED");
        assertThat(logger.single().level()).isEqualTo(System.Logger.Level.INFO);
    }

    @Test
    void shouldLogAndRethrowWhenHandlerThrows() {
        var boom = new IllegalStateException("database is locked");
        var handler = decorate((ScheduleAppointment c) -> {
            throw boom;
        });

        assertThatThrownBy(() -> handler.handle(new ScheduleAppointment("09:00", SENSITIVE)))
            .isSameAs(boom);

        assertThat(logger.single().message())
            .contains("outcome=error")
            .contains("exception=IllegalStateException");
        assertThat(logger.single().level()).isEqualTo(System.Logger.Level.ERROR);
        assertThat(logger.single().thrown()).isSameAs(boom);
    }

    @Test
    void shouldNotIncludeCorrelationId() {
        var handler = decorate((ScheduleAppointment c) -> Result.success("ok"));

        CorrelationContext.runWith("a3f9c1", () -> handler.handle(new ScheduleAppointment("09:00", SENSITIVE)));

        assertThat(logger.single().message()).doesNotContain("correlationId=");
    }
}
