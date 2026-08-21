package atlas.presentation.nutrition.web;

import atlas.presentation.common.web.StaticResources;
import atlas.presentation.sharedkernel.http.HttpRequest;
import atlas.presentation.sharedkernel.http.HttpResponse;
import java.util.Map;

public final class DocsHandlers {

    private static final String SWAGGER_PAGE = StaticResources.read("/web-nutrition/swagger.html");
    private static final String OPENAPI_SPEC = StaticResources.read("/web-nutrition/openapi.json");

    private DocsHandlers() {}

    public static HttpResponse docs(HttpRequest request) {
        return new HttpResponse(200, "text/html; charset=utf-8", SWAGGER_PAGE, Map.of());
    }

    public static HttpResponse openapi(HttpRequest request) {
        return HttpResponse.json(200, OPENAPI_SPEC);
    }
}
