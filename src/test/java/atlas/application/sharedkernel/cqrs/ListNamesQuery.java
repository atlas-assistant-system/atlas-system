package atlas.application.sharedkernel.cqrs;

import java.util.List;

record ListNamesQuery(String prefix) implements Query<List<String>> {}
