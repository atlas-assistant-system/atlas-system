package atlas.presentation.appointments.web;

import atlas.presentation.common.web.StaticResources;
import atlas.presentation.sharedkernel.http.HttpRequest;
import atlas.presentation.sharedkernel.http.HttpResponse;
import java.util.Map;

public final class UiHandlers {

    private static final String PAGE = StaticResources.read("/web-appointments/app.html");
    private static final String STYLES = StaticResources.read("/web-appointments/app.css");
    private static final String SCRIPT = StaticResources.read("/web-appointments/app.js");

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
