package atlas.app.core;

import atlas.presentation.common.web.ErrorMessagesHandler;
import atlas.presentation.core.web.UiHandlers;
import atlas.presentation.sharedkernel.http.Router;
import atlas.presentation.sharedkernel.http.Routes;

public final class CoreApplication {

    private final Router router;

    private CoreApplication(Router router) {
        this.router = router;
    }

    public static CoreApplication wire() {
        return new CoreApplication(Router.builder()
            .mount(Routes.at("/")
                .get("/", UiHandlers::index)
                .get("/assets/errors.js", ErrorMessagesHandler::script)
                .get("/assets/app.css", UiHandlers::styles)
                .get("/assets/app.js", UiHandlers::script)
                .get("/assets/routines.css", UiHandlers::routinesStyles)
                .get("/assets/routines.js", UiHandlers::routinesScript)
                .get("/assets/economy.css", UiHandlers::economyStyles)
                .get("/assets/economy.js", UiHandlers::economyScript)
                .get("/assets/nutrition.css", UiHandlers::nutritionStyles)
                .get("/assets/nutrition.js", UiHandlers::nutritionScript)
                .get("/assets/training.css", UiHandlers::trainingStyles)
                .get("/assets/training.js", UiHandlers::trainingScript))
            .build());
    }

    public Router router() {
        return router;
    }
}
