package atlas.presentation.presence.requests;

import java.time.Instant;

public record CompleteAuthentication(
    String modelVersion,
    float[] descriptor,
    String observedType,
    String nonce,
    Instant capturedAt) {}
