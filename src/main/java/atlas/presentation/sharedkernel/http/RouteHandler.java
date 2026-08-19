package atlas.presentation.sharedkernel.http;

@FunctionalInterface
public interface RouteHandler {

    HttpResponse handle(HttpRequest request);
}
