package atlas.presentation.routines.web;

import atlas.presentation.common.web.StaticResources;

import java.util.Map;
import sharedkernel.presentation.http.HttpRequest;
import sharedkernel.presentation.http.HttpResponse;

public final class DocsHandlers {

    private static final String SWAGGER_PAGE = StaticResources.read("/web-routines/swagger.html");
    private static final String OPENAPI_SPEC = StaticResources.read("/web-routines/openapi.json");

    private DocsHandlers() {}

    public static HttpResponse docs(HttpRequest request) {
        return new HttpResponse(200, "text/html; charset=utf-8", SWAGGER_PAGE, Map.of());
    }

    public static HttpResponse openapi(HttpRequest request) {
        return HttpResponse.json(200, OPENAPI_SPEC);
    }
}
