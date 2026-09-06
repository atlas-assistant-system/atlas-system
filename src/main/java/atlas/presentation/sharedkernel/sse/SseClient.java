package atlas.presentation.sharedkernel.sse;

import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.guards.StringGuard;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SseClient implements AutoCloseable {

    private final String id;
    private final OutputStream output;
    private final CountDownLatch closeSignal = new CountDownLatch(1);
    private final AtomicBoolean closed = new AtomicBoolean();

    private volatile boolean open = true;

    public SseClient(String id, OutputStream output) {
        this.id = StringGuard.notBlank(id, "id");
        this.output = ObjectGuard.notNull(output, "output");
    }

    public String id() {
        return id;
    }

    public boolean isOpen() {
        return open;
    }

    public void send(SseEvent event) throws IOException {
        ObjectGuard.notNull(event, "event");

        write(event.toWireFormat());
    }

    public void comment(String text) throws IOException {
        write(": " + text.replace('\n', ' ').replace('\r', ' ') + "\n\n");
    }

    public void awaitClose() throws InterruptedException {
        closeSignal.await();
    }

    @Override
    public void close() {
        open = false;

        if (!closed.compareAndSet(false, true)) {
            return;
        }

        closeSignal.countDown();

        try {
            output.close();
        } catch (IOException | RuntimeException ignored) {}
    }

    private void write(String frame) throws IOException {
        if (!open) {
            throw new IOException("SSE client " + id + " is already closed");
        }

        try {
            output.write(frame.getBytes(StandardCharsets.UTF_8));
            output.flush();
        } catch (IOException e) {
            open = false;
            throw e;
        }
    }
}
