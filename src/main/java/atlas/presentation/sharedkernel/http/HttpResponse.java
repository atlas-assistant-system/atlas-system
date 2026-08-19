package atlas.presentation.sharedkernel.http;

import atlas.domain.sharedkernel.results.Error;
import atlas.presentation.sharedkernel.errors.ApiError;
import atlas.presentation.sharedkernel.errors.FieldError;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record HttpResponse(int status, String contentType, String body, Map<String, String> headers) {

    public static final String JSON = "application/json;charset=UTF-8";
    public static final String TEXT = "text/plain;charset=UTF-8";

    public HttpResponse {
        body = body == null ? "" : body;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public static HttpResponse ok(String jsonBody) {
        return json(200, jsonBody);
    }

    public static HttpResponse created(String location, String jsonBody) {
        return json(201, jsonBody).withHeader("Location", location);
    }

    public static HttpResponse noContent() {
        return new HttpResponse(204, null, "", Map.of());
    }

    public static HttpResponse json(int status, String body) {
        return new HttpResponse(status, JSON, body, Map.of());
    }

    public static HttpResponse text(int status, String body) {
        return new HttpResponse(status, TEXT, body, Map.of());
    }

    public static HttpResponse error(Error error) {
        return error(ApiError.from(error));
    }

    public static HttpResponse error(Error error, List<FieldError> fieldErrors) {
        return error(ApiError.from(error, fieldErrors));
    }

    public static HttpResponse error(Throwable thrown) {
        return error(ApiError.from(thrown));
    }

    public static HttpResponse error(ApiError error) {
        return json(error.status(), ApiErrorJson.write(error));
    }

    public HttpResponse withHeader(String name, String value) {
        var merged = new LinkedHashMap<>(headers);
        merged.put(name, value);

        return new HttpResponse(status, contentType, body, merged);
    }
}
