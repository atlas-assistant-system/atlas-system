package atlas.presentation.sharedkernel.sse;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

final class CountingStream extends OutputStream {

    final AtomicInteger closes = new AtomicInteger();

    @Override
    public void write(int b) {}

    @Override
    public void close() {
        closes.incrementAndGet();
    }
}
