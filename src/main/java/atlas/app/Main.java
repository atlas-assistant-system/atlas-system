package atlas.app;

import atlas.app.presence.PresenceSettings;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.time.Clock;
import java.util.logging.LogManager;
import sharedkernel.infrastructure.console.StartupBanner;
import sharedkernel.infrastructure.logging.LogEntryRenderers;

public final class Main {

    public static final int DEFAULT_PORT = 8080;

    private Main() {}

    public static void main(String[] args) throws IOException {
        configureLogging();

        var application = Application
            .wire(LogEntryRenderers.forCurrentConsole(), PresenceSettings.fromEnvironment(args),
                Clock.systemDefaultZone())
            .start(DEFAULT_PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(application::stop));

        System.out.print(banner(application.port()));
    }

    private static String banner(int port) {
        return StartupBanner
            .named("Atlas")
            .with("JDK", StartupBanner.jdkVersion())
            .with("Port", String.valueOf(port))
            .with("PID", StartupBanner.processId())
            .with("API", "http://localhost:" + port)
            .render();
    }

    private static void configureLogging() {
        try (var config = Main.class.getResourceAsStream("/logging.properties")) {
            if (config != null) {
                LogManager.getLogManager().readConfiguration(config);
            }
        } catch (IOException e) {
            System.getLogger("atlas").log(Level.WARNING, "Falling back to default logging.", e);
        }
    }
}
