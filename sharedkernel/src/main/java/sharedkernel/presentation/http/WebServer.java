package sharedkernel.presentation.http;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import sharedkernel.domain.guards.ObjectGuard;
import sharedkernel.domain.guards.StringGuard;

public final class WebServer implements AutoCloseable {

    public static final int SHUTDOWN_GRACE_IN_SECONDS = 2;

    private final HttpServer server;

    private WebServer(HttpServer server) {
        this.server = server;
    }

    public static WebServer onLoopback(int port) throws IOException {
        var address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
        var server = HttpServer.create(address, 0);

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        return new WebServer(server);
    }

    public WebServer mount(String path, HttpHandler handler) {
        StringGuard.notBlank(path, "path");
        ObjectGuard.notNull(handler, "handler");

        server.createContext(path, handler);

        return this;
    }

    public WebServer start() {
        server.start();

        return this;
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(SHUTDOWN_GRACE_IN_SECONDS);
    }
}
