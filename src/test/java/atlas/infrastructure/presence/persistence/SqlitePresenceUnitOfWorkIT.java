package atlas.infrastructure.presence.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.presence.ports.AuthenticationAttemptReadModel;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.application.sharedkernel.paging.PageRequest;
import atlas.domain.presence.AuthenticationSession;
import atlas.domain.presence.BiometricProfile;
import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.entities.FaceTemplateId;
import atlas.domain.presence.enums.VerificationOutcome;
import atlas.domain.presence.events.AuthenticationFailedEvent;
import atlas.domain.presence.events.AuthenticationSucceededEvent;
import atlas.domain.presence.events.LivenessChallengeIssuedEvent;
import atlas.domain.presence.events.ProfileEnrolledEvent;
import atlas.domain.presence.vos.FaceDescriptor;
import atlas.domain.presence.vos.ModelVersion;
import atlas.domain.presence.vos.ProfileName;
import atlas.domain.presence.vos.SessionDuration;
import atlas.domain.sharedkernel.events.DomainEvent;
import atlas.domain.sharedkernel.results.Result;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.infrastructure.presence.memory.InMemoryLivenessChallengeRepository;
import atlas.infrastructure.sharedkernel.persistence.SchemaMigrator;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqlitePresenceUnitOfWorkIT {

    private static final Instant NOW = Instant.parse("2026-08-19T10:00:00Z");
    private static final ModelVersion MODEL = ModelVersion.of("face-v1");
    private static final FaceTemplateId TEMPLATE_ID = FaceTemplateId.of(new UUID(0, 1));

    private Connection connection;
    private SqlitePresenceUnitOfWork unitOfWork;
    private AuthenticationAttemptReadModel attempts;
    private InMemoryLivenessChallengeRepository challenges;
    private final List<DomainEvent> published = new ArrayList<>();

    @BeforeEach
    void openDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        connection.setAutoCommit(false);
        new SchemaMigrator(connection, Clock.fixed(NOW, ZoneOffset.UTC)).migrate(PresenceMigrations.load());

        var publisher = new SimpleDomainEventPublisher();
        publisher.subscribe(ProfileEnrolledEvent.class, published::add);
        publisher.subscribe(AuthenticationFailedEvent.class, published::add);
        publisher.subscribe(AuthenticationSucceededEvent.class, published::add);
        publisher.subscribe(LivenessChallengeIssuedEvent.class, published::add);
        var dispatcher = new PendingEventDispatcher(publisher);

        unitOfWork = new SqlitePresenceUnitOfWork(
            connection,
            new AuthenticationAuditDelivery(connection, dispatcher),
            new SqliteSequenceGenerator(connection));
        attempts = new SqliteAuthenticationAttemptReadModel(connection);
        challenges = new InMemoryLivenessChallengeRepository();
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        connection.close();
    }

    @Test
    void shouldPersistAndRehydrateAProfileWithoutChangingItsDescriptor() {
        var id = unitOfWork.execute(() -> {
            var profile = profile(unitOfWork.profiles().nextId());
            unitOfWork.profiles().create(profile);
            return profile.id();
        });

        var loaded = unitOfWork.profiles().get(id).orElseThrow();

        assertThat(loaded.displayName()).isEqualTo(ProfileName.of("Ada"));
        assertThat(loaded.templates()).singleElement().satisfies(template -> {
            assertThat(template.id()).isEqualTo(TEMPLATE_ID);
            assertThat(template.descriptor()).isEqualTo(FaceDescriptor.of(MODEL, new float[]{1.25f, -2.5f}));
            assertThat(template.capturedAt()).isEqualTo(NOW);
        });
        assertThat(published).singleElement().isInstanceOf(ProfileEnrolledEvent.class);
    }

    @Test
    void shouldReplaceTemplatesOnUpdateAndDeleteThemWithTheProfile() throws SQLException {
        var id = persistProfile();
        var secondTemplate = FaceTemplateId.of(new UUID(0, 2));

        unitOfWork.run(() -> {
            var profile = unitOfWork.profiles().get(id).orElseThrow();
            profile.addTemplate(secondTemplate, FaceDescriptor.of(MODEL, new float[]{0.5f, 0.5f}), NOW.plusSeconds(1));
            unitOfWork.profiles().update(profile);
        });

        assertThat(unitOfWork.profiles().get(id).orElseThrow().templates()).hasSize(2);

        unitOfWork.run(() -> unitOfWork.profiles().delete(unitOfWork.profiles().get(id).orElseThrow()));

        assertThat(unitOfWork.profiles().get(id)).isEmpty();
        assertThat(count("face_templates")).isZero();
    }

    @Test
    void shouldPersistSessionsAndFindOnlyActiveOnes() {
        var profileId = persistProfile();

        var activeId = unitOfWork.execute(() -> {
            var active = AuthenticationSession.open(
                unitOfWork.sessions().nextId(), profileId, SessionDuration.of(Duration.ofMinutes(15)), NOW);
            unitOfWork.sessions().create(active);

            var closed = AuthenticationSession.open(
                unitOfWork.sessions().nextId(), profileId, SessionDuration.of(Duration.ofMinutes(15)), NOW);
            closed.close(NOW.plusSeconds(1));
            unitOfWork.sessions().create(closed);
            return active.id();
        });

        assertThat(unitOfWork.sessions().findActive()).extracting(AuthenticationSession::id).containsExactly(activeId);
        assertThat(unitOfWork.sessions().findActiveByProfile(profileId)).map(AuthenticationSession::id)
            .contains(activeId);
    }

    @Test
    void shouldPersistTheGateAndAppendFailuresAndSuccessesToTheAudit() {
        var profileId = persistProfile();

        unitOfWork.run(() -> {
            var gate = unitOfWork.gate().get();
            gate.registerFailure(VerificationOutcome.NO_MATCH, NOW.plusSeconds(1));
            unitOfWork.gate().save(gate);
        });

        unitOfWork.run(() -> {
            var session = AuthenticationSession.open(
                unitOfWork.sessions().nextId(), profileId, SessionDuration.of(Duration.ofMinutes(15)), NOW);
            unitOfWork.sessions().create(session);
            var gate = unitOfWork.gate().get();
            gate.registerSuccess(profileId, session.id(), NOW.plusSeconds(2));
            unitOfWork.gate().save(gate);
        });

        assertThat(unitOfWork.gate().get().failedAttempts()).isZero();
        var page = attempts.find(PageRequest.of(1, 10));
        assertThat(page.totalCount()).isEqualTo(2);
        assertThat(page.items()).extracting(attempt -> attempt.outcome())
            .containsExactly(VerificationOutcome.MATCHED, VerificationOutcome.NO_MATCH);
        assertThat(published).filteredOn(AuthenticationFailedEvent.class::isInstance).hasSize(1);
        assertThat(published).filteredOn(AuthenticationSucceededEvent.class::isInstance).hasSize(1);
    }

    @Test
    void shouldRollBackPersistenceAndEventsWhenWorkReturnsAFailure() {
        var result = unitOfWork.execute(() -> {
            unitOfWork.profiles().create(profile(unitOfWork.profiles().nextId()));
            return Result.failure(atlas.domain.presence.PresenceErrors.PROFILE_NAME_REQUIRED);
        });

        assertThat(result.isFailure()).isTrue();
        assertThat(unitOfWork.profiles().getAll()).isEmpty();
        assertThat(published).isEmpty();
    }

    @Test
    void shouldKeepChallengesInMemoryAndPublishTheirEventsAfterCommit() {
        var id = challenges.nextId();
        var challenge = atlas.domain.presence.LivenessChallenge.issue(
            id,
            atlas.domain.presence.enums.LivenessChallengeType.VICTORY,
            atlas.domain.presence.vos.ChallengeNonce.of("00000000000000000000000000000000"),
            NOW);

        unitOfWork.run(() -> {
            challenges.save(challenge);
            assertThat(published).isEmpty();
        });

        assertThat(challenges.get(id)).contains(challenge);
        assertThat(published).singleElement().isInstanceOf(LivenessChallengeIssuedEvent.class);
        assertThat(challenges.removeExpired(NOW.plusSeconds(10))).isEqualTo(1);
        assertThat(challenges.get(id)).isEmpty();
    }

    @Test
    void shouldGenerateUniqueChallengeIdsConcurrently() {
        var ids = IntStream.range(0, 1_000).parallel().mapToObj(ignored -> challenges.nextId()).toList();

        assertThat(new HashSet<>(ids)).hasSize(1_000);
    }

    private BiometricProfileId persistProfile() {
        return unitOfWork.execute(() -> {
            var profile = profile(unitOfWork.profiles().nextId());
            unitOfWork.profiles().create(profile);
            return profile.id();
        });
    }

    private static BiometricProfile profile(BiometricProfileId id) {
        return BiometricProfile.enroll(
            id, ProfileName.of("Ada"), TEMPLATE_ID, FaceDescriptor.of(MODEL, new float[]{1.25f, -2.5f}), NOW);
    }

    private long count(String table) throws SQLException {
        try (var statement = connection.createStatement();
            var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next();
            return rows.getLong(1);
        }
    }
}
