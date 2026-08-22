package atlas.presentation.sharedkernel.http;

import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.util.ArrayList;
import java.util.List;

public final class RouterBuilder {

    private final List<Route> routes = new ArrayList<>();

    RouterBuilder() {}

    public RouterBuilder mount(Routes mounted) {
        ObjectGuard.notNull(mounted, "mounted");

        routes.addAll(mounted.all());

        return this;
    }

    public Router build() {
        return new Router(List.copyOf(routes));
    }
}
