package atlas.application.nutrition.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.nutrition.commands.correctweighin.CorrectWeighInCommand;
import atlas.application.nutrition.commands.correctweighin.CorrectWeighInCommandHandler;
import atlas.application.nutrition.commands.deleteweighin.DeleteWeighInCommand;
import atlas.application.nutrition.commands.deleteweighin.DeleteWeighInCommandHandler;
import atlas.application.nutrition.commands.recordweighin.RecordWeighInCommand;
import atlas.application.nutrition.commands.recordweighin.RecordWeighInCommandHandler;
import atlas.application.nutrition.ports.NutritionUnitOfWork;
import atlas.application.nutrition.ports.WeighInRepository;
import atlas.domain.nutrition.NutritionErrors;
import atlas.domain.nutrition.WeighIn;
import atlas.domain.nutrition.WeighInErrors;
import atlas.domain.nutrition.WeighInId;
import atlas.domain.nutrition.events.WeighInDeletedEvent;
import atlas.domain.nutrition.events.WeighInRecordedEvent;
import atlas.domain.nutrition.vos.Weight;
import atlas.support.builders.UnitOfWorkStub;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WeighInCommandHandlersTest {

    private static final Instant NOW = Instant.parse("2026-08-22T07:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final WeighInId ID = WeighInId.of(7);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final NutritionUnitOfWork unitOfWork = mock(NutritionUnitOfWork.class);
    private final WeighInRepository weighIns = mock(WeighInRepository.class);

    private final RecordWeighInCommandHandler record = new RecordWeighInCommandHandler(unitOfWork, CLOCK);
    private final CorrectWeighInCommandHandler correct = new CorrectWeighInCommandHandler(unitOfWork, CLOCK);
    private final DeleteWeighInCommandHandler delete = new DeleteWeighInCommandHandler(unitOfWork, CLOCK);

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.withWeighIns(unitOfWork, weighIns);
        when(weighIns.nextId()).thenReturn(ID);
        when(weighIns.findOn(any())).thenReturn(Optional.empty());
    }

    @Test
    void shouldPersistTheReading() {
        var result = record.handle(new RecordWeighInCommand(new BigDecimal("82.4"), TODAY));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().id()).isEqualTo("W00000007");
        assertThat(result.value().weight()).isEqualByComparingTo("82.4");
        assertThat(result.value().measuredOn()).isEqualTo(TODAY);

        verify(weighIns).create(any(WeighIn.class));
    }

    @Test
    void shouldRegisterTheRecordedEvent() {
        record.handle(new RecordWeighInCommand(new BigDecimal("82.4"), TODAY));

        var saved = ArgumentCaptor.forClass(WeighIn.class);
        verify(weighIns).create(saved.capture());

        assertThat(saved.getValue().pendingEvents()).containsExactly(new WeighInRecordedEvent(ID, NOW));
    }

    @Test
    void shouldDateTheReadingTodayWhenNoDateIsGiven() {
        var result = record.handle(new RecordWeighInCommand(new BigDecimal("82.4"), null));

        assertThat(result.value().measuredOn()).isEqualTo(TODAY);
    }

    @Test
    void shouldCorrectTheReadingOfTheDayInsteadOfAddingASecondOne() {
        var existing = existingReading(TODAY);
        when(weighIns.findOn(TODAY)).thenReturn(Optional.of(existing));

        var result = record.handle(new RecordWeighInCommand(new BigDecimal("82.1"), TODAY));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().weight()).isEqualByComparingTo("82.1");
        assertThat(existing.weight()).isEqualTo(new Weight(82_100));

        verify(weighIns).update(existing);
        verify(weighIns, never()).create(any());
    }

    @Test
    void shouldFailWhenTheWeightIsNotBelievable() {
        var result = record.handle(new RecordWeighInCommand(new BigDecimal("900.0"), TODAY));

        assertThat(result.error()).isEqualTo(NutritionErrors.WEIGHT_OUT_OF_RANGE);
        verify(weighIns, never()).create(any());
    }

    @Test
    void shouldFailWhenTheReadingIsDatedInTheFuture() {
        var result = record.handle(new RecordWeighInCommand(new BigDecimal("82.4"), TODAY.plusDays(1)));

        assertThat(result.error()).isEqualTo(WeighInErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
        verify(weighIns, never()).create(any());
    }

    @Test
    void shouldCorrectAnEarlierReading() {
        var existing = existingReading(TODAY.minusDays(3));
        when(weighIns.get(ID)).thenReturn(Optional.of(existing));

        var result = correct.handle(new CorrectWeighInCommand(ID, new BigDecimal("83.0")));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().weight()).isEqualByComparingTo("83");
        assertThat(result.value().measuredOn()).isEqualTo(TODAY.minusDays(3));

        verify(weighIns).update(existing);
    }

    @Test
    void shouldFailToCorrectAReadingThatIsNotThere() {
        when(weighIns.get(ID)).thenReturn(Optional.empty());

        var result = correct.handle(new CorrectWeighInCommand(ID, new BigDecimal("83.0")));

        assertThat(result.error()).isEqualTo(WeighInErrors.notFound(ID));
        verify(weighIns, never()).update(any());
    }

    @Test
    void shouldDeleteTheReadingAndRegisterItsEvent() {
        var existing = existingReading(TODAY);
        when(weighIns.get(ID)).thenReturn(Optional.of(existing));

        var result = delete.handle(new DeleteWeighInCommand(ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(existing.pendingEvents()).contains(new WeighInDeletedEvent(ID, NOW));

        verify(weighIns).delete(existing);
    }

    @Test
    void shouldFailToDeleteAReadingThatIsNotThere() {
        when(weighIns.get(ID)).thenReturn(Optional.empty());

        var result = delete.handle(new DeleteWeighInCommand(ID));

        assertThat(result.error()).isEqualTo(WeighInErrors.notFound(ID));
        verify(weighIns, never()).delete(any());
    }

    private static WeighIn existingReading(LocalDate day) {
        return WeighIn.rehydrate(ID, new Weight(82_400), day, NOW);
    }
}
