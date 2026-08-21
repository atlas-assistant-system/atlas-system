package atlas.presentation.common.web;

import atlas.presentation.sharedkernel.http.HttpRequest;
import atlas.presentation.sharedkernel.http.HttpResponse;
import java.util.Map;

/**
 * Sirve la traducción de los códigos del catálogo de errores. La tabla es la misma para
 * cualquier página —el código `Routine.IsArchived` significa lo mismo se mire desde donde se
 * mire—, así que se sirve desde un único sitio en vez de copiarse en cada JS.
 */
public final class ErrorMessagesHandler {

    private static final String SCRIPT = StaticResources.read("/web-shared/error-messages.js");

    private ErrorMessagesHandler() {}

    public static HttpResponse script(HttpRequest request) {
        return new HttpResponse(200, "text/javascript; charset=utf-8", SCRIPT, Map.of());
    }
}
