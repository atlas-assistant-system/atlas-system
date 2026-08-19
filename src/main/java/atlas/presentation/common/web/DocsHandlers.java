package atlas.presentation.common.web;

import java.util.Map;
import sharedkernel.presentation.http.HttpRequest;
import sharedkernel.presentation.http.HttpResponse;

public final class DocsHandlers {

    private static final String SWAGGER_PAGE = StaticResources.read("/web-presence/swagger.html");
    private static final String OPENAPI_SPEC = StaticResources.read("/web-presence/openapi.json");

    private DocsHandlers() {}

    public static HttpResponse docs(HttpRequest request) {
        return new HttpResponse(200, "text/html; charset=utf-8", SWAGGER_PAGE, Map.of());
    }

    public static HttpResponse openapi(HttpRequest request) {
        return HttpResponse.json(200, OPENAPI_SPEC);
    }
}
