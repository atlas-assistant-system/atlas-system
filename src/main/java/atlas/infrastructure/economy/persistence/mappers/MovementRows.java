package atlas.infrastructure.economy.persistence.mappers;

import atlas.domain.economy.Movement;
import atlas.domain.economy.MovementId;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import atlas.domain.economy.vos.Money;
import atlas.domain.economy.vos.MovementNote;
import atlas.domain.sharedkernel.results.Result;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;

public final class MovementRows {

    private MovementRows() {}

    public static Movement toMovement(ResultSet row) throws SQLException {
        var signedCents = row.getLong("amount_cents");
        var kind = MovementKind.of(signedCents);
        var amount = require(Money.ofCents(Math.abs(signedCents)), "movements.amount_cents");
        var note = require(MovementNote.create(row.getString("note")), "movements.note");

        return Movement.rehydrate(
            MovementId.of(row.getLong("id")),
            kind,
            amount,
            Category.valueOf(row.getString("category")),
            note,
            LocalDate.parse(row.getString("occurred_on")),
            Instant.parse(row.getString("recorded_at")));
    }

    public static void bind(PreparedStatement statement, Movement movement) throws SQLException {
        statement.setLong(1, movement.id().value());
        statement.setLong(2, movement.kind().signed(movement.amount()));
        statement.setString(3, movement.amount().currency());
        statement.setString(4, movement.category().name());
        statement.setString(5, movement.note().map(MovementNote::value).orElse(null));
        statement.setString(6, movement.occurredOn().toString());
        statement.setString(7, movement.recordedAt().toString());
    }

    private static <T> T require(Result<T> result, String column) {
        if (result.isFailure()) {
            throw new PersistenceException("Corrupt value in " + column + ": " + result.error().message());
        }

        return result.value();
    }
}
