package atlas.presentation.sharedkernel.sse;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SseHeaders {

    public static final String CONTENT_TYPE = "text/event-stream;charset=UTF-8";
    public static final String LAST_EVENT_ID = "Last-Event-ID";

    private static final Map<String, String> REQUIRED = Map.copyOf(required());

    private SseHeaders() {}

    public static Map<String, String> forStream() {
        return REQUIRED;
    }

    private static Map<String, String> required() {
        var headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", CONTENT_TYPE);
        headers.put("Cache-Control", "no-cache, no-transform");
        headers.put("Connection", "keep-alive");
        headers.put("X-Accel-Buffering", "no");

        return headers;
    }
}
