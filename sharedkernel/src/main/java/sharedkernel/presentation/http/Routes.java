package sharedkernel.presentation.http;

import java.util.ArrayList;
import java.util.List;
import sharedkernel.domain.guards.ObjectGuard;
import sharedkernel.domain.guards.StringGuard;

public final class Routes {

    private final String prefix;
    private final List<Route> routes = new ArrayList<>();

    private Routes(String prefix) {
        this.prefix = prefix;
    }

    public static Routes at(String prefix) {
        StringGuard.notBlank(prefix, "prefix");

        if (!prefix.startsWith("/")) {
            throw new IllegalArgumentException("A mount prefix must start with '/': " + prefix);
        }

        if (prefix.indexOf('{') >= 0) {
            throw new IllegalArgumentException("A mount prefix must be literal, without path parameters: " + prefix);
        }

        return new Routes(prefix);
    }

    public Routes get(String pattern, RouteHandler handler) {
        return add("GET", pattern, handler);
    }

    public Routes post(String pattern, RouteHandler handler) {
        return add("POST", pattern, handler);
    }

    public Routes put(String pattern, RouteHandler handler) {
        return add("PUT", pattern, handler);
    }

    public Routes delete(String pattern, RouteHandler handler) {
        return add("DELETE", pattern, handler);
    }

    List<Route> all() {
        return List.copyOf(routes);
    }

    private Routes add(String method, String pattern, RouteHandler handler) {
        ObjectGuard.notNull(pattern, "pattern");
        ObjectGuard.notNull(handler, "handler");

        routes.add(new Route(method, PathSegments.of(prefix + "/" + pattern), handler));

        return this;
    }
}
