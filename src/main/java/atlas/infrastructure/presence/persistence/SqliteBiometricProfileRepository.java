package atlas.infrastructure.presence.persistence;

import atlas.application.presence.ports.BiometricProfileRepository;
import atlas.domain.presence.BiometricProfile;
import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.entities.FaceTemplate;
import atlas.domain.presence.entities.FaceTemplateId;
import atlas.domain.presence.vos.FaceDescriptor;
import atlas.domain.presence.vos.ModelVersion;
import atlas.domain.presence.vos.ProfileName;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.AbstractSqlRepository;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SqliteBiometricProfileRepository
    extends AbstractSqlRepository<BiometricProfile, BiometricProfileId>
    implements BiometricProfileRepository {

    private static final String SEQUENCE_NAME = "profiles";

    private final SequenceGenerator sequences;

    public SqliteBiometricProfileRepository(Connection connection, SequenceGenerator sequences) {
        super(connection, "profiles", "id");
        this.sequences = sequences;
    }

    @Override
    public BiometricProfileId nextId() {
        return BiometricProfileId.of(sequences.next(SEQUENCE_NAME));
    }

    @Override
    public void create(BiometricProfile profile) {
        super.create(profile);
        insertTemplates(profile);
    }

    @Override
    public void update(BiometricProfile profile) {
        super.update(profile);
        deleteTemplatesOf(profile.id());
        insertTemplates(profile);
    }

    @Override
    public void delete(BiometricProfile profile) {
        deleteTemplatesOf(profile.id());
        super.delete(profile);
    }

    @Override
    protected List<String> columns() {
        return List.of("id", "display_name");
    }

    @Override
    protected void bind(PreparedStatement statement, BiometricProfile profile) throws SQLException {
        statement.setLong(1, profile.id().value());
        statement.setString(2, profile.displayName().value());
    }

    @Override
    protected BiometricProfile mapRow(ResultSet row) throws SQLException {
        var id = BiometricProfileId.of(row.getLong("id"));
        return BiometricProfile.rehydrate(id, ProfileName.of(row.getString("display_name")), templatesOf(id));
    }

    @Override
    protected Object idValue(BiometricProfileId id) {
        return id.value();
    }

    private List<FaceTemplate> templatesOf(BiometricProfileId profileId) throws SQLException {
        var sql = "SELECT id, descriptor, model_version, captured_at FROM face_templates"
            + " WHERE profile_id = ? ORDER BY captured_at, id";

        try (var statement = connection().prepareStatement(sql)) {
            statement.setLong(1, profileId.value());

            try (var rows = statement.executeQuery()) {
                var templates = new ArrayList<FaceTemplate>();
                while (rows.next()) {
                    var descriptor = FaceDescriptor.of(
                        ModelVersion.of(rows.getString("model_version")),
                        FaceDescriptors.decode(rows.getBytes("descriptor")));
                    templates.add(FaceTemplate.create(
                        FaceTemplateId.of(UUID.fromString(rows.getString("id"))),
                        descriptor,
                        Instant.parse(rows.getString("captured_at"))));
                }

                return List.copyOf(templates);
            }
        }
    }

    private void insertTemplates(BiometricProfile profile) {
        var sql = "INSERT INTO face_templates (id, profile_id, descriptor, model_version, captured_at)"
            + " VALUES (?, ?, ?, ?, ?)";

        try (var statement = connection().prepareStatement(sql)) {
            for (var template : profile.templates()) {
                statement.setString(1, template.id().value().toString());
                statement.setLong(2, profile.id().value());
                statement.setBytes(3, FaceDescriptors.encode(template.descriptor().values()));
                statement.setString(4, template.modelVersion().value());
                statement.setString(5, template.capturedAt().toString());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to persist the face templates of " + profile.id(), e);
        }
    }

    private void deleteTemplatesOf(BiometricProfileId id) {
        try (var statement = connection().prepareStatement("DELETE FROM face_templates WHERE profile_id = ?")) {
            statement.setLong(1, id.value());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to delete the face templates of " + id, e);
        }
    }
}
