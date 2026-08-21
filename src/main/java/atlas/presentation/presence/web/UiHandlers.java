package atlas.presentation.presence.web;

import atlas.presentation.common.web.StaticResources;
import atlas.presentation.sharedkernel.http.HttpRequest;
import atlas.presentation.sharedkernel.http.HttpResponse;
import java.util.Map;

public final class UiHandlers {

    private static final String PAGE = StaticResources.read("/web-presence/app.html");
    private static final String STYLES = StaticResources.read("/web-presence/app.css");
    private static final String SCRIPT = StaticResources.read("/web-presence/app.js");
    private static final String FACE_QUALITY_SCRIPT = StaticResources.read("/web-presence/face-quality.js");
    private static final String SANDBOX_PAGE = StaticResources.read("/web-presence/sandbox.html");
    private static final String SANDBOX_STYLES = StaticResources.read("/web-presence/sandbox.css");
    private static final String SANDBOX_SCRIPT = StaticResources.read("/web-presence/sandbox.js");

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

    public static HttpResponse faceQualityScript(HttpRequest request) {
        return new HttpResponse(200, "text/javascript; charset=utf-8", FACE_QUALITY_SCRIPT, Map.of());
    }

    public static HttpResponse sandbox(HttpRequest request) {
        return new HttpResponse(200, "text/html; charset=utf-8", SANDBOX_PAGE, Map.of());
    }

    public static HttpResponse sandboxStyles(HttpRequest request) {
        return new HttpResponse(200, "text/css; charset=utf-8", SANDBOX_STYLES, Map.of());
    }

    public static HttpResponse sandboxScript(HttpRequest request) {
        return new HttpResponse(200, "text/javascript; charset=utf-8", SANDBOX_SCRIPT, Map.of());
    }
}
