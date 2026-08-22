package atlas.presentation.sharedkernel.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

class SseClientTest {

    @Test
    void shouldCloseTheUnderlyingStreamOnlyOnce() {
        var stream = new CountingStream();
        var client = new SseClient("abc", stream);

        client.close();
        client.close();

        assertThat(stream.closes).hasValue(1);
    }

    @Test
    void shouldReleaseWaitersWhenClosed() throws InterruptedException {
        var client = new SseClient("abc", new ByteArrayOutputStream());

        client.close();
        client.awaitClose();

        assertThat(client.isOpen()).isFalse();
    }

    @Test
    void shouldNotPropagateFailuresRaisedWhileClosing() {
        var client = new SseClient("abc", new OutputStream() {

            @Override
            public void write(int b) {}

            @Override
            public void close() throws IOException {
                throw new IOException("already gone");
            }
        });

        assertThatCode(client::close).doesNotThrowAnyException();
    }
}
