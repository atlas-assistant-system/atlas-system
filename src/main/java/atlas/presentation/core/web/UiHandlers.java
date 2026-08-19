package atlas.presentation.core.web;

import atlas.presentation.common.web.StaticResources;
import atlas.presentation.sharedkernel.http.HttpRequest;
import atlas.presentation.sharedkernel.http.HttpResponse;
import java.util.Map;

public final class UiHandlers {

    private static final String PAGE = StaticResources.read("/web-core/app.html");
    private static final String STYLES = StaticResources.read("/web-core/app.css");
    private static final String SCRIPT = StaticResources.read("/web-core/app.js");
    private static final String ROUTINES_STYLES = StaticResources.read("/web-core/routines.css");
    private static final String ROUTINES_SCRIPT = StaticResources.read("/web-core/routines.js");

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

    public static HttpResponse routinesStyles(HttpRequest request) {
        return new HttpResponse(200, "text/css; charset=utf-8", ROUTINES_STYLES, Map.of());
    }

    public static HttpResponse routinesScript(HttpRequest request) {
        return new HttpResponse(200, "text/javascript; charset=utf-8", ROUTINES_SCRIPT, Map.of());
    }
}
