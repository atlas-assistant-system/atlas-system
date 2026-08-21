package atlas.domain.nutrition.services;

import atlas.domain.nutrition.Plan;
import atlas.domain.nutrition.WeighIn;
import atlas.domain.nutrition.vos.Progress;
import atlas.domain.nutrition.vos.Weight;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.OptionalDouble;

public final class PlanProgress {

    public static final int TREND_WINDOW_DAYS = 7;

    private static final int FULL = 100;

    public Progress of(Plan plan, Collection<WeighIn> series, LocalDate today) {
        var current = latestOf(series, plan.startWeight());
        var target = plan.targetWeight();

        return new Progress(
            plan.startWeight(),
            current,
            target,
            plan.goal(),
            current.gramsTo(target),
            percentageOf(plan, current),
            plan.goal().isReached(current, target),
            trendOf(series, today));
    }

    private static Weight latestOf(Collection<WeighIn> series, Weight fallback) {
        return series.stream()
            .max(Comparator.comparing(WeighIn::measuredOn))
            .map(WeighIn::weight)
            .orElse(fallback);
    }

    private static int percentageOf(Plan plan, Weight current) {
        var total = plan.startWeight().gramsTo(plan.targetWeight());
        if (total == 0) {
            return plan.goal().isReached(current, plan.targetWeight()) ? FULL : 0;
        }

        var travelled = plan.startWeight().gramsTo(current);

        return clamp(Math.round(FULL * (float) travelled / total));
    }

    private static int clamp(int percentage) {
        return Math.max(0, Math.min(FULL, percentage));
    }

    private static int trendOf(Collection<WeighIn> series, LocalDate today) {
        var recent = averageBetween(series, today.minusDays(TREND_WINDOW_DAYS - 1L), today);
        var previous = averageBetween(
            series, today.minusDays(2L * TREND_WINDOW_DAYS - 1), today.minusDays(TREND_WINDOW_DAYS));

        if (recent.isEmpty() || previous.isEmpty()) {
            return 0;
        }

        return (int) Math.round(recent.getAsDouble() - previous.getAsDouble());
    }

    private static OptionalDouble averageBetween(Collection<WeighIn> series, LocalDate from, LocalDate to) {
        return series.stream()
            .filter(weighIn -> !weighIn.measuredOn().isBefore(from) && !weighIn.measuredOn().isAfter(to))
            .mapToInt(weighIn -> weighIn.weight().grams())
            .average();
    }
}
