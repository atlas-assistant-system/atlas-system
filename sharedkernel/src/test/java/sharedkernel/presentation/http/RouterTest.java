package sharedkernel.presentation.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import sharedkernel.application.logging.CorrelationContext;
import sharedkernel.domain.exceptions.FormatException;
import sharedkernel.presentation.errors.ExceptionTranslator;

class RouterTest {

    private static HttpRequest request(String method, String path) {
        return HttpRequest.of(method, path);
    }

    private static HttpRequest request(String method, String path, Map<String, String> headers) {
        return new HttpRequest(method, path, Map.of(), Map.of(), headers, "");
    }

    @Test
    void shouldRouteToTheHandlerMountedAtItsPrefix() {
        var router = Router.builder()
            .mount(Routes.at("/appointments").get("/", r -> HttpResponse.ok("[]")))
            .build();

        var response = router.dispatch(request("GET", "/appointments"));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("[]");
    }

    @Test
    void shouldExtractPathParametersWhenTheRouteDeclaresThem() {
        var router = Router.builder()
            .mount(Routes.at("/appointments").get("/{id}", r -> HttpResponse.ok(r.pathParam("id"))))
            .build();

        var response = router.dispatch(request("GET", "/appointments/A00000007"));

        assertThat(response.body()).isEqualTo("A00000007");
    }

    @Test
    void shouldKeepEachBoundedContextUnderItsOwnPrefix() {
        var router = Router.builder()
            .mount(Routes.at("/appointments").get("/{id}", r -> HttpResponse.ok("appointment")))
            .mount(Routes.at("/reminders").get("/{id}", r -> HttpResponse.ok("reminder")))
            .build();

        assertThat(router.dispatch(request("GET", "/appointments/1")).body()).isEqualTo("appointment");
        assertThat(router.dispatch(request("GET", "/reminders/1")).body()).isEqualTo("reminder");
    }

    @Test
    void shouldNotMatchASiblingPrefixThatSharesTheSameStart() {
        var router = Router.builder()
            .mount(Routes.at("/appointments").get("/", r -> HttpResponse.ok("appointments")))
            .build();

        assertThat(router.dispatch(request("GET", "/appointmentsx")).status()).isEqualTo(404);
    }

    @Test
    void shouldReturnNotFoundWhenNoRouteMatches() {
        var router = Router.builder().mount(Routes.at("/appointments").get("/", r -> HttpResponse.ok("[]"))).build();

        var response = router.dispatch(request("GET", "/unknown"));

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.body()).contains(Router.ROUTE_NOT_FOUND);
    }

    @Test
    void shouldReturnMethodNotAllowedWhenOnlyTheMethodDiffers() {
        var router = Router.builder()
            .mount(Routes.at("/appointments").post("/", r -> HttpResponse.noContent()))
            .build();

        var response = router.dispatch(request("DELETE", "/appointments"));

        assertThat(response.status()).isEqualTo(405);
        assertThat(response.headers()).containsEntry("Allow", "POST");
    }

    @Test
    void shouldHideTheDetailWhenAHandlerThrowsUnexpectedly() {
        var router = Router.builder()
            .mount(Routes.at("/appointments").get("/", r -> {
                throw new IllegalStateException("jdbc:sqlite:/secret/path failed");
            }))
            .build();

        var response = router.dispatch(request("GET", "/appointments"));

        assertThat(response.status()).isEqualTo(500);
        assertThat(response.body()).doesNotContain("secret").contains(ExceptionTranslator.UNEXPECTED_MESSAGE);
    }

    @Test
    void shouldTranslateMalformedInputToBadRequest() {
        var router = Router.builder()
            .mount(Routes.at("/appointments").post("/", r -> {
                throw new FormatException("Not a valid appointment id.");
            }))
            .build();

        var response = router.dispatch(request("POST", "/appointments"));

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.body()).contains(ExceptionTranslator.MALFORMED_INPUT);
    }

    @Test
    void shouldBindACorrelationIdWhileTheHandlerRuns() {
        var router = Router.builder()
            .mount(Routes.at("/appointments").get("/", r -> HttpResponse.ok(CorrelationContext.current().orElse(""))))
            .build();

        assertThat(router.dispatch(request("GET", "/appointments")).body()).isNotBlank();
    }

    @Test
    void shouldKeepTheCorrelationIdSuppliedByTheCaller() {
        var router = Router.builder()
            .mount(Routes.at("/appointments").get("/", r -> HttpResponse.ok(CorrelationContext.current().orElse(""))))
            .build();

        var response = router.dispatch(request("GET", "/appointments", Map.of(Router.CORRELATION_HEADER, "abc123")));

        assertThat(response.body()).isEqualTo("abc123");
    }

    @Test
    void shouldReplaceACorrelationIdThatWouldForgeAHeader() {
        var forged = "abc\r\nX-Injected: 1";
        var router = Router.builder()
            .mount(Routes.at("/appointments").get("/", r -> HttpResponse.ok(CorrelationContext.current().orElse(""))))
            .build();

        var response = router.dispatch(request("GET", "/appointments", Map.of(Router.CORRELATION_HEADER, forged)));

        assertThat(response.body()).isNotEqualTo(forged).doesNotContain("\n");
    }

    @Test
    void shouldDecodePathParametersWhenTheyArePercentEncoded() {
        var router = Router.builder()
            .mount(Routes.at("/notes").get("/{title}", r -> HttpResponse.ok(r.pathParam("title"))))
            .build();

        assertThat(router.dispatch(request("GET", "/notes/a%2Fb")).body()).isEqualTo("a/b");
    }
}
