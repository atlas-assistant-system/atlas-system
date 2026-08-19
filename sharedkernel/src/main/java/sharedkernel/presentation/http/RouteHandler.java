package sharedkernel.presentation.http;

@FunctionalInterface
public interface RouteHandler {

    HttpResponse handle(HttpRequest request);
}
