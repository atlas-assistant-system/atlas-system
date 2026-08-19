package atlas.presentation.common.web;

import java.util.Map;
import sharedkernel.presentation.http.HttpRequest;
import sharedkernel.presentation.http.HttpResponse;

public final class UiHandlers {

    private static final String PAGE = StaticResources.read("/web-presence/app.html");
    private static final String STYLES = StaticResources.read("/web-presence/app.css");
    private static final String SCRIPT = StaticResources.read("/web-presence/app.js");

    private UiHandlers() {}

    public static HttpResponse index(HttpRequest request) {
        return new HttpResponse(200, "text/html; charset=utf-8", PAGE, Map.of());
    }

    public static HttpResponse styles(HttpRequest request) {
        return new HttpResponse(200, "text/css; charset=utf-8", STYLES, Map.of());
    }

    public static HttpResponse script(HttpRequest request) {
        return new HttpResponse(200, "text/javascript; charset=utf-8", SCRIPT, Map.of());
    }
}
