package atlas.application.presence.dto;

public record AuthenticationStateDto(
    int failedAttempts,
    int enrolledProfiles,
    SessionDto activeSession) {}
