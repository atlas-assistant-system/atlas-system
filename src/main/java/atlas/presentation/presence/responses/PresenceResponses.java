package atlas.presentation.presence.responses;

import atlas.application.presence.dto.AuthenticationAttemptDto;
import atlas.application.presence.dto.AuthenticationStateDto;
import atlas.application.presence.dto.ChallengeDto;
import atlas.application.presence.dto.FaceTemplateDto;
import atlas.application.presence.dto.ProfileDto;
import atlas.application.presence.dto.ProfileSummaryDto;
import atlas.application.presence.dto.SessionDto;
import atlas.application.sharedkernel.paging.Page;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PresenceResponses {

    private PresenceResponses() {}

    public static Map<String, Object> profile(ProfileDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("displayName", dto.displayName());
        body.put("modelVersion", dto.modelVersion());
        body.put("templateCount", dto.templateCount());
        body.put("templates", dto.templates().stream().map(PresenceResponses::template).toList());
        return body;
    }

    public static Map<String, Object> profileSummary(ProfileSummaryDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("displayName", dto.displayName());
        body.put("templateCount", dto.templateCount());
        return body;
    }

    public static List<Map<String, Object>> profileSummaries(List<ProfileSummaryDto> profiles) {
        return profiles.stream().map(PresenceResponses::profileSummary).toList();
    }

    public static Map<String, Object> challenge(ChallengeDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("challengeId", dto.challengeId());
        body.put("type", dto.type());
        body.put("nonce", dto.nonce());
        body.put("expiresAt", dto.expiresAt().toString());
        return body;
    }

    public static Map<String, Object> session(SessionDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("profileId", dto.profileId());
        body.put("openedAt", dto.openedAt().toString());
        body.put("lastActivityAt", dto.lastActivityAt().toString());
        body.put("expiresAt", dto.expiresAt().toString());
        body.put("status", dto.status());
        return body;
    }

    public static Object optionalSession(Optional<SessionDto> session) {
        return session.<Object>map(PresenceResponses::session).orElse(null);
    }

    public static Map<String, Object> authenticationState(AuthenticationStateDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("failedAttempts", dto.failedAttempts());
        body.put("enrolledProfiles", dto.enrolledProfiles());
        body.put("activeSession", dto.activeSession() == null ? null : session(dto.activeSession()));
        return body;
    }

    public static Map<String, Object> attempts(Page<AuthenticationAttemptDto> page) {
        var body = new LinkedHashMap<String, Object>();
        body.put("items", page.items().stream().map(PresenceResponses::attempt).toList());
        body.put("pageNumber", page.pageNumber());
        body.put("pageSize", page.pageSize());
        body.put("totalCount", page.totalCount());
        body.put("totalPages", page.totalPages());
        return body;
    }

    private static Map<String, Object> template(FaceTemplateDto dto) {
        return Map.of("id", dto.id(), "capturedAt", dto.capturedAt().toString());
    }

    private static Map<String, Object> attempt(AuthenticationAttemptDto dto) {
        return Map.of("occurredOn", dto.occurredOn().toString(), "outcome", dto.outcome());
    }

    private static String instant(Instant value) {
        return value == null ? null : value.toString();
    }
}
