package atlas.presentation.presence.requests;

import atlas.presentation.presence.web.Values;
import java.util.Map;

public final class PresenceRequests {

    private PresenceRequests() {}

    public static EnrollProfile enrollProfile(Map<String, Object> body) {
        return new EnrollProfile(
            Values.text(body, "displayName"),
            Values.text(body, "modelVersion"),
            Values.floats(body, "descriptor"));
    }

    public static FaceTemplate faceTemplate(Map<String, Object> body) {
        return new FaceTemplate(Values.text(body, "modelVersion"), Values.floats(body, "descriptor"));
    }

    public static CompleteAuthentication completeAuthentication(Map<String, Object> body) {
        return new CompleteAuthentication(
            Values.text(body, "modelVersion"),
            Values.floats(body, "descriptor"),
            Values.text(body, "observedType"),
            Values.text(body, "nonce"),
            Values.instant(body, "capturedAt"));
    }
}
