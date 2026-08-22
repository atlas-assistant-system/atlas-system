package atlas.presentation.sharedkernel.sse;

import java.io.IOException;
import java.io.OutputStream;

final class BrokenPipe extends OutputStream {

    @Override
    public void write(int b) throws IOException {
        throw new IOException("broken pipe");
    }
}
