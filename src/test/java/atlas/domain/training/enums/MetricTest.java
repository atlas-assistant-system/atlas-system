package atlas.domain.training.enums;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.training.vos.Effort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MetricTest {

    private static final Effort PRESS = new Effort(70_000, 8, 0, 0);
    private static final Effort HEAVIER = new Effort(80_000, 5, 0, 0);
    private static final Effort PULL_UPS = new Effort(0, 12, 0, 0);
    private static final Effort PLANK = new Effort(0, 0, 45, 0);
    private static final Effort RUN = new Effort(0, 0, 1_560, 5_000);

    @Test
    void shouldScoreLoadByTheWeightOnTheBar() {
        assertThat(Metric.LOAD.scoreOf(PRESS)).isEqualTo(70_000);
        assertThat(Metric.LOAD.scoreOf(HEAVIER)).isEqualTo(80_000);
    }

    @Test
    void shouldScoreTheOtherMetricsByTheirOwnMeasure() {
        assertThat(Metric.REPS.scoreOf(PULL_UPS)).isEqualTo(12);
        assertThat(Metric.TIME.scoreOf(PLANK)).isEqualTo(45);
        assertThat(Metric.DISTANCE.scoreOf(RUN)).isEqualTo(5_000);
    }

    @Test
    void shouldMultiplyLoadByRepsForVolumeBecauseThatIsTheWorkDone() {
        assertThat(Metric.LOAD.volumeOf(PRESS)).isEqualTo(560_000L);
        assertThat(Metric.LOAD.volumeOf(HEAVIER)).isEqualTo(400_000L);
    }

    @Test
    void shouldSumTheOwnMeasureForTheOtherMetrics() {
        assertThat(Metric.REPS.volumeOf(PULL_UPS)).isEqualTo(12L);
        assertThat(Metric.TIME.volumeOf(PLANK)).isEqualTo(45L);
        assertThat(Metric.DISTANCE.volumeOf(RUN)).isEqualTo(5_000L);
    }

    @Test
    void shouldNotOverflowTheVolumeOfAHardYear() {
        var heaviest = new Effort(Effort.MAX_LOAD_GRAMS, 1_000, 0, 0);

        assertThat(Metric.LOAD.volumeOf(heaviest)).isEqualTo(500_000_000L);
    }

    @Test
    void shouldPickTheBestSetByItsOwnScore() {
        assertThat(Metric.LOAD.bestOf(List.of(PRESS, HEAVIER))).contains(HEAVIER);
        assertThat(Metric.REPS.bestOf(List.of(PULL_UPS, new Effort(0, 9, 0, 0)))).contains(PULL_UPS);
    }

    @Test
    void shouldHaveNoBestWhenNothingWasDone() {
        assertThat(Metric.LOAD.bestOf(List.of())).isEmpty();
    }

    @Test
    void shouldKeepTheFirstOfTwoEquallyGoodSets() {
        var first = new Effort(70_000, 8, 0, 0);
        var second = new Effort(70_000, 3, 0, 0);

        assertThat(Metric.LOAD.bestOf(List.of(first, second)).orElseThrow().reps()).isEqualTo(8);
    }

    @ParameterizedTest
    @EnumSource(Metric.class)
    void shouldCarryALabelForTheMirror(Metric metric) {
        assertThat(metric.label()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(Metric.class)
    void shouldSurvivePersistenceByName(Metric metric) {
        assertThat(Metric.valueOf(metric.name())).isEqualTo(metric);
    }

    @ParameterizedTest
    @EnumSource(Metric.class)
    void shouldScoreAnEmptyEffortAsNothing(Metric metric) {
        assertThat(metric.scoreOf(Effort.NONE)).isZero();
        assertThat(metric.volumeOf(Effort.NONE)).isZero();
    }
}
