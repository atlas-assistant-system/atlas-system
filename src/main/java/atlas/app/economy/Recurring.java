package atlas.app.economy;

import atlas.application.economy.commands.recordmovement.RecordMovementCommand;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

record Recurring(
    MovementKind kind, Category category, String note, int dayOfMonth, long baseCents, long spreadCents) {

    RecordMovementCommand on(LocalDate month, Random random) {
        var cents = baseCents + (spreadCents == 0 ? 0 : random.nextLong(spreadCents));

        return new RecordMovementCommand(
            kind, BigDecimal.valueOf(cents, 2), category, note, month.withDayOfMonth(dayOfMonth));
    }
}
