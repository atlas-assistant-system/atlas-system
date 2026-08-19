package atlas.app.core;

import atlas.presentation.core.web.UiHandlers;
import atlas.presentation.sharedkernel.http.Router;
import atlas.presentation.sharedkernel.http.Routes;

public final class CoreApplication {

    private final Router router;

    private CoreApplication(Router router) {
        this.router = router;
    }

    public static CoreApplication wire(CoreSettings settings) {
        return new CoreApplication(Router.builder()
            .mount(Routes.at("/")
                .get("/", UiHandlers::index)
                .get("/assets/app.css", UiHandlers::styles)
                .get("/assets/app.js", UiHandlers::script)
                .get("/assets/config.js", request -> UiHandlers.config(settings.latitude(), settings.longitude()))
                .get("/assets/routines.css", UiHandlers::routinesStyles)
                .get("/assets/routines.js", UiHandlers::routinesScript))
            .build());
    }

    public Router router() {
        return router;
    }
}
