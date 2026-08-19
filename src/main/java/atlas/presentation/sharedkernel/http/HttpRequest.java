package atlas.presentation.sharedkernel.http;

import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public record HttpRequest(
    String method,
    String path,
    Map<String, String> pathParams,
    Map<String, String> queryParams,
    Map<String, String> headers,
    String body) {

    public HttpRequest {
        ObjectGuard.notNull(method, "method");
        ObjectGuard.notNull(path, "path");

        pathParams = pathParams == null ? Map.of() : Map.copyOf(pathParams);
        queryParams = queryParams == null ? Map.of() : Map.copyOf(queryParams);
        headers = caseInsensitiveCopyOf(headers);
        body = body == null ? "" : body;
    }

    public static HttpRequest of(String method, String path) {
        return new HttpRequest(method, path, Map.of(), Map.of(), Map.of(), "");
    }

    public static HttpRequest of(String method, String path, String body) {
        return new HttpRequest(method, path, Map.of(), Map.of(), Map.of(), body);
    }

    public String pathParam(String name) {
        var value = pathParams.get(name);

        if (value == null) {
            throw new IllegalArgumentException("The matched route declares no path parameter named '" + name + "'.");
        }

        return value;
    }

    public Optional<String> queryParam(String name) {
        return Optional.ofNullable(queryParams.get(name));
    }

    public Optional<String> header(String name) {
        return Optional.ofNullable(headers.get(name));
    }

    public HttpRequest withPathParams(Map<String, String> params) {
        return new HttpRequest(method, path, params, queryParams, headers, body);
    }

    private static Map<String, String> caseInsensitiveCopyOf(Map<String, String> headers) {
        var copy = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        if (headers != null) {
            copy.putAll(headers);
        }

        return Collections.unmodifiableMap(copy);
    }
}
