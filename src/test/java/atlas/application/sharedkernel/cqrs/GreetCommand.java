package atlas.application.sharedkernel.cqrs;

record GreetCommand(String name) implements Command<String> {}
