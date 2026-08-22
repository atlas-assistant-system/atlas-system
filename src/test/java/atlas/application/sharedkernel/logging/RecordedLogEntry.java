package atlas.application.sharedkernel.logging;

record RecordedLogEntry(System.Logger.Level level, String message, Throwable thrown) {}
