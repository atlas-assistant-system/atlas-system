package sharedkernel.application.logging;

public interface LogEntryRenderer {

    String render(HandlerLogEntry entry);
}
