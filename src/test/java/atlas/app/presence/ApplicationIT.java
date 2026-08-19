package atlas.app.presence;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.presence.commands.beginauthentication.BeginAuthenticationCommand;
import atlas.application.presence.commands.completeauthentication.CompleteAuthenticationCommand;
import atlas.application.presence.commands.enrollprofile.EnrollProfileCommand;
import atlas.application.presence.queries.getauthenticationstate.GetAuthenticationStateQuery;
import atlas.application.presence.queries.listauthenticationattempts.ListAuthenticationAttemptsQuery;
import atlas.application.presence.queries.listprofiles.ListProfilesQuery;
import atlas.application.sharedkernel.logging.PlainLogEntryRenderer;
import atlas.application.sharedkernel.paging.PageRequest;
import atlas.domain.presence.LivenessChallengeId;
import atlas.domain.presence.enums.VerificationOutcome;
import atlas.domain.presence.vos.MatchThreshold;
import atlas.domain.presence.vos.SessionDuration;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApplicationIT {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-19T10:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path dataDirectory;

    @Test
    void shouldWireHandlersMigrateSQLiteAndKeepDataAcrossRestarts() {
        var settings = new PresenceSettings(
            0,
            dataDirectory,
            true,
            MatchThreshold.of(0.8),
            SessionDuration.of(Duration.ofMinutes(15)));

        var first = PresenceApplication.wire(new PlainLogEntryRenderer(), settings, CLOCK);
        var enrolled = first.commands().dispatch(new EnrollProfileCommand(
            "Ada", "face-v1", new float[]{1.0f, 0.0f}));
        assertThat(enrolled.isSuccess()).isTrue();

        var challenge = first.commands().dispatch(new BeginAuthenticationCommand()).value();
        var rejected = first.commands().dispatch(new CompleteAuthenticationCommand(
            LivenessChallengeId.parse(challenge.challengeId()),
            "face-v1",
            new float[]{20.0f, 20.0f},
            challenge.type(),
            challenge.nonce(),
            CLOCK.instant()));
        assertThat(rejected.isFailure()).isTrue();
        assertThat(first.queries().dispatch(new GetAuthenticationStateQuery()).value().failedAttempts()).isEqualTo(1);
        assertThat(first.queries()
            .dispatch(new ListAuthenticationAttemptsQuery(PageRequest.first()))
            .value()
            .items())
            .singleElement()
            .extracting(attempt -> attempt.outcome())
            .isEqualTo(VerificationOutcome.NO_MATCH.name());
        first.stop();

        var second = PresenceApplication.wire(new PlainLogEntryRenderer(), settings, CLOCK);
        var profiles = second.queries().dispatch(new ListProfilesQuery());

        assertThat(profiles.value()).singleElement().satisfies(profile -> {
            assertThat(profile.displayName()).isEqualTo("Ada");
            assertThat(profile.templateCount()).isEqualTo(1);
        });
        assertThat(dataDirectory.resolve("presence.db")).exists();
        second.stop();
    }
}
