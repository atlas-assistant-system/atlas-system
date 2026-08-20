package atlas.app.economy;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.infrastructure.sharedkernel.logging.LogEntryRenderers;
import com.fasterxml.jackson.jr.ob.JSON;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HttpApiIT {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    @TempDir
    static Path databaseDirectory;

    private static EconomyApplication application;
    private static HttpClient client;
    private static String base;

    @BeforeAll
    static void startServer() throws IOException {
        application =
            EconomyApplication.wire(LogEntryRenderers.forConsole(false, ZoneOffset.UTC), databaseDirectory, CLOCK)
                .start(0);
        client = HttpClient.newHttpClient();
        base = "http://localhost:" + application.port();
    }

    @AfterAll
    static void stopServer() {
        application.stop();
    }

    @Test
    void shouldWalkTheWholeLifeOfAMovement() throws Exception {
        var created = send("POST", "/economy/movements", """
            {"kind":"EXPENSE","amount":"12.50","category":"FOOD","note":"cena","occurredOn":"2026-08-05"}""");
        assertThat(created.statusCode()).isEqualTo(201);

        var movement = json(created);
        var id = (String) movement.get("id");
        assertThat(id).matches("M\\d{8}");
        assertThat(movement.get("amount")).isEqualTo("12.50");
        assertThat(movement.get("kind")).isEqualTo("EXPENSE");
        assertThat(movement.get("category")).isEqualTo("FOOD");
        assertThat(movement.get("categoryLabel")).isEqualTo("Comida");
        assertThat(movement.get("note")).isEqualTo("cena");
        assertThat(movement.get("occurredOn")).isEqualTo("2026-08-05");

        assertThat(json(send("GET", "/economy/movements/" + id, null)).get("amount")).isEqualTo("12.50");
        assertThat(jsonList(send("GET", "/economy/movements", null))).hasSize(1);

        var corrected = json(send("PUT", "/economy/movements/" + id, """
            {"amount":"20.00","note":"cena y postre","occurredOn":"2026-08-06"}"""));
        assertThat(corrected.get("amount")).isEqualTo("20.00");
        assertThat(corrected.get("occurredOn")).isEqualTo("2026-08-06");

        var recategorized = json(send("PUT", "/economy/movements/" + id + "/category", """
            {"category":"LEISURE"}"""));
        assertThat(recategorized.get("category")).isEqualTo("LEISURE");
        assertThat(recategorized.get("categoryLabel")).isEqualTo("Ocio");

        assertThat(send("DELETE", "/economy/movements/" + id, null).statusCode()).isEqualTo(204);
        assertThat(send("GET", "/economy/movements/" + id, null).statusCode()).isEqualTo(404);
        assertThat(jsonList(send("GET", "/economy/movements", null))).isEmpty();
    }

    @Test
    void shouldReportTheBalanceAndTheBreakdownOfTheCurrentMonth() throws Exception {
        var salary = recordMovement("INCOME", "2000.00", "INCOME", "2026-08-01");
        var groceries = recordMovement("EXPENSE", "75.00", "FOOD", "2026-08-05");
        var cinema = recordMovement("EXPENSE", "25.00", "LEISURE", "2026-08-12");

        var balance = json(send("GET", "/economy/balance", null));
        assertThat(balance.get("from")).isEqualTo("2026-08-01");
        assertThat(balance.get("to")).isEqualTo("2026-08-31");
        assertThat(balance.get("income")).isEqualTo("2000.00");
        assertThat(balance.get("expense")).isEqualTo("100.00");
        assertThat(balance.get("net")).isEqualTo("1900.00");

        var breakdown = jsonList(send("GET", "/economy/breakdown", null));
        assertThat(breakdown).hasSize(2);
        assertThat(breakdown.getFirst().get("category")).isEqualTo("FOOD");
        assertThat(breakdown.getFirst().get("total")).isEqualTo("75.00");
        assertThat(breakdown.getFirst().get("percentage")).isEqualTo("75.0");
        assertThat(breakdown.getLast().get("category")).isEqualTo("LEISURE");

        var lastYear = json(send("GET", "/economy/balance?from=2025-01-01&to=2025-12-31", null));
        assertThat(lastYear.get("net")).isEqualTo("0.00");

        remove(salary, groceries, cinema);
    }

    @Test
    void shouldLeaveTheBalanceAtZeroWhenIncomeAndSpendingMatch() throws Exception {
        var income = recordMovement("INCOME", "49.99", "INCOME", "2026-08-03");
        var expense = recordMovement("EXPENSE", "49.99", "FOOD", "2026-08-03");

        assertThat(json(send("GET", "/economy/balance", null)).get("net")).isEqualTo("0.00");

        remove(income, expense);
    }

    @Test
    void shouldFilterTheListByCategoryAndPeriod() throws Exception {
        var food = recordMovement("EXPENSE", "10.00", "FOOD", "2026-08-05");
        var leisure = recordMovement("EXPENSE", "20.00", "LEISURE", "2026-08-06");

        assertThat(jsonList(send("GET", "/economy/movements?category=LEISURE", null))).hasSize(1);
        assertThat(jsonList(send("GET", "/economy/movements?from=2026-08-06&to=2026-08-06", null))).hasSize(1);
        assertThat(jsonList(send("GET", "/economy/movements?limit=1", null))).hasSize(1);

        remove(food, leisure);
    }

    @Test
    void shouldDateAMovementTodayWhenNoDateIsGiven() throws Exception {
        var response = json(send("POST", "/economy/movements", """
            {"kind":"EXPENSE","amount":"5.00","category":"OTHER"}"""));

        assertThat(response.get("occurredOn")).isEqualTo("2026-08-20");
        assertThat(response.get("note")).isNull();

        remove((String) response.get("id"));
    }

    @Test
    void shouldRejectACategoryThatBelongsToTheOtherKind() throws Exception {
        var response = send("POST", "/economy/movements", """
            {"kind":"EXPENSE","amount":"12.50","category":"INCOME"}""");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("Movement.CategoryDoesNotMatchKind");
    }

    @Test
    void shouldRejectAMovementDatedInTheFuture() throws Exception {
        var response = send("POST", "/economy/movements", """
            {"kind":"EXPENSE","amount":"12.50","category":"FOOD","occurredOn":"2026-08-21"}""");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("Movement.CannotBeDatedInTheFuture");
    }

    @Test
    void shouldRejectAnAmountThatIsNotPositive() throws Exception {
        var response = send("POST", "/economy/movements", """
            {"kind":"EXPENSE","amount":"0","category":"FOOD"}""");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("Movement.AmountMustBePositive");
    }

    @Test
    void shouldRejectAnAmountThatIsNotANumber() throws Exception {
        var response = send("POST", "/economy/movements", """
            {"kind":"EXPENSE","amount":"doce euros","category":"FOOD"}""");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void shouldRejectAnUnknownCategory() throws Exception {
        var response = send("POST", "/economy/movements", """
            {"kind":"EXPENSE","amount":"12.50","category":"CRIPTO"}""");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void shouldReportAnUnknownMovementAsNotFound() throws Exception {
        var response = send("GET", "/economy/movements/M00009999", null);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(json(response).get("code")).isEqualTo("Movement.NotFound");
    }

    @Test
    void shouldWalkTheWholeLifeOfABudget() throws Exception {
        var created = send("POST", "/economy/budgets", """
            {"category":"FOOD","limit":"300.00"}""");
        assertThat(created.statusCode()).isEqualTo(201);

        var budget = json(created);
        var id = (String) budget.get("id");
        assertThat(id).matches("P\\d{8}");
        assertThat(budget.get("category")).isEqualTo("FOOD");
        assertThat(budget.get("label")).isEqualTo("Comida");
        assertThat(budget.get("limit")).isEqualTo("300.00");

        var raised = json(send("PUT", "/economy/budgets/" + id, """
            {"limit":"500.00"}"""));
        assertThat(raised.get("limit")).isEqualTo("500.00");

        assertThat(jsonList(send("GET", "/economy/budgets", null))).hasSize(1);

        assertThat(send("DELETE", "/economy/budgets/" + id, null).statusCode()).isEqualTo(204);
        assertThat(jsonList(send("GET", "/economy/budgets", null))).isEmpty();
    }

    @Test
    void shouldReportWhatEachBudgetHasCommittedThisMonth() throws Exception {
        var budget = (String) json(send("POST", "/economy/budgets", """
            {"category":"FOOD","limit":"200.00"}""")).get("id");
        var lunch = recordMovement("EXPENSE", "180.00", "FOOD", "2026-08-05");

        var status = jsonList(send("GET", "/economy/budgets", null)).getFirst();
        assertThat(status.get("limit")).isEqualTo("200.00");
        assertThat(status.get("spent")).isEqualTo("180.00");
        assertThat(status.get("projected")).isEqualTo("279.00");
        assertThat(status.get("status")).isEqualTo("AT_RISK");

        send("DELETE", "/economy/budgets/" + budget, null);
        remove(lunch);
    }

    @Test
    void shouldRefuseASecondBudgetForTheSameCategory() throws Exception {
        var first = (String) json(send("POST", "/economy/budgets", """
            {"category":"LEISURE","limit":"50.00"}""")).get("id");

        var second = send("POST", "/economy/budgets", """
            {"category":"LEISURE","limit":"90.00"}""");

        assertThat(second.statusCode()).isEqualTo(409);
        assertThat(json(second).get("code")).isEqualTo("Budget.AlreadyDefined");

        send("DELETE", "/economy/budgets/" + first, null);
    }

    @Test
    void shouldRefuseToBudgetIncome() throws Exception {
        var response = send("POST", "/economy/budgets", """
            {"category":"INCOME","limit":"300.00"}""");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("Budget.OnlySpendingCanBeBudgeted");
    }

    @Test
    void shouldReportAnUnknownBudgetAsNotFound() throws Exception {
        var response = send("GET", "/economy/budgets", null);
        assertThat(response.statusCode()).isEqualTo(200);

        assertThat(send("DELETE", "/economy/budgets/P00009999", null).statusCode()).isEqualTo(404);
    }

    @Test
    void shouldServeItsDocsBelowTheModulePath() throws Exception {
        assertThat(send("GET", "/", null).statusCode()).isEqualTo(404);
        assertThat(send("GET", "/economy/docs", null).body()).contains("swagger");
        assertThat(json(send("GET", "/economy/openapi.json", null))).containsKey("paths");
    }

    private static String recordMovement(String kind, String amount, String category, String day) throws Exception {
        var response = send("POST", "/economy/movements", """
            {"kind":"%s","amount":"%s","category":"%s","occurredOn":"%s"}"""
            .formatted(kind, amount, category, day));

        return (String) json(response).get("id");
    }

    private static void remove(String... ids) throws Exception {
        for (var id : ids) {
            send("DELETE", "/economy/movements/" + id, null);
        }
    }

    private static HttpResponse<String> send(String method, String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + path))
            .header("Content-Type", "application/json")
            .method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body))
            .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static Map<String, Object> json(HttpResponse<String> response) throws IOException {
        return JSON.std.mapFrom(response.body());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> jsonList(HttpResponse<String> response) throws IOException {
        return (List<Map<String, Object>>) (List<?>) JSON.std.listFrom(response.body());
    }
}
