package atlas.infrastructure.sharedkernel.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

final class NoteRepository extends AbstractSqlRepository<Note, NoteId> {

    NoteRepository(Connection connection) {
        super(connection, "notes", "id");
    }

    @Override
    protected List<String> columns() {
        return List.of("id", "text");
    }

    @Override
    protected void bind(PreparedStatement statement, Note note) throws SQLException {
        statement.setLong(1, note.id().value());
        statement.setString(2, note.text());
    }

    @Override
    protected Note mapRow(ResultSet row) throws SQLException {
        return new Note(row.getLong("id"), row.getString("text"));
    }

    @Override
    protected Object idValue(NoteId id) {
        return id.value();
    }
}
