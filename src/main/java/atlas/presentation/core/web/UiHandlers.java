package atlas.presentation.core.web;

import atlas.presentation.common.web.StaticResources;
import atlas.presentation.sharedkernel.http.HttpRequest;
import atlas.presentation.sharedkernel.http.HttpResponse;
import java.util.Map;

public final class UiHandlers {

    private static final String PAGE = StaticResources.read("/web-core/app.html");
    private static final String STYLES = StaticResources.read("/web-core/app.css");
    private static final String SCRIPT = StaticResources.read("/web-core/app.js");
    private static final String INTERACTION_SCRIPT = StaticResources.read("/web-core/interaction.js");
    private static final String ROUTINES_STYLES = StaticResources.read("/web-core/routines.css");
    private static final String ROUTINES_SCRIPT = StaticResources.read("/web-core/routines.js");
    private static final String ECONOMY_STYLES = StaticResources.read("/web-core/economy.css");
    private static final String ECONOMY_SCRIPT = StaticResources.read("/web-core/economy.js");
    private static final String NUTRITION_STYLES = StaticResources.read("/web-core/nutrition.css");
    private static final String NUTRITION_SCRIPT = StaticResources.read("/web-core/nutrition.js");
    private static final String TRAINING_STYLES = StaticResources.read("/web-core/training.css");
    private static final String TRAINING_SCRIPT = StaticResources.read("/web-core/training.js");

    private UiHandlers() {}

    public static HttpResponse index(HttpRequest request) {
        return new HttpResponse(200, "text/html; charset=utf-8", PAGE, Map.of());
    }

    public static HttpResponse styles(HttpRequest request) {
        return new HttpResponse(200, "text/css; charset=utf-8", STYLES, Map.of());
    }

    public static HttpResponse script(HttpRequest request) {
        return new HttpResponse(200, "text/javascript; charset=utf-8", SCRIPT, Map.of());
    }

    public static HttpResponse interactionScript(HttpRequest request) {
        return new HttpResponse(200, "text/javascript; charset=utf-8", INTERACTION_SCRIPT, Map.of());
    }

    public static HttpResponse routinesStyles(HttpRequest request) {
        return new HttpResponse(200, "text/css; charset=utf-8", ROUTINES_STYLES, Map.of());
    }

    public static HttpResponse routinesScript(HttpRequest request) {
        return new HttpResponse(200, "text/javascript; charset=utf-8", ROUTINES_SCRIPT, Map.of());
    }

    public static HttpResponse economyStyles(HttpRequest request) {
        return new HttpResponse(200, "text/css; charset=utf-8", ECONOMY_STYLES, Map.of());
    }

    public static HttpResponse economyScript(HttpRequest request) {
        return new HttpResponse(200, "text/javascript; charset=utf-8", ECONOMY_SCRIPT, Map.of());
    }

    public static HttpResponse nutritionStyles(HttpRequest request) {
        return new HttpResponse(200, "text/css; charset=utf-8", NUTRITION_STYLES, Map.of());
    }

    public static HttpResponse nutritionScript(HttpRequest request) {
        return new HttpResponse(200, "text/javascript; charset=utf-8", NUTRITION_SCRIPT, Map.of());
    }

    public static HttpResponse trainingStyles(HttpRequest request) {
        return new HttpResponse(200, "text/css; charset=utf-8", TRAINING_STYLES, Map.of());
    }

    public static HttpResponse trainingScript(HttpRequest request) {
        return new HttpResponse(200, "text/javascript; charset=utf-8", TRAINING_SCRIPT, Map.of());
    }
}
