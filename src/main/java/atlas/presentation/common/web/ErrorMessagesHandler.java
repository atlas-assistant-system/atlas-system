package atlas.presentation.common.web;

import atlas.presentation.sharedkernel.http.HttpRequest;
import atlas.presentation.sharedkernel.http.HttpResponse;
import java.util.Map;

public final class ErrorMessagesHandler {

    private static final String SCRIPT = StaticResources.read("/web-shared/error-messages.js");

    private ErrorMessagesHandler() {}

    public static HttpResponse script(HttpRequest request) {
        return new HttpResponse(200, "text/javascript; charset=utf-8", SCRIPT, Map.of());
    }
}
