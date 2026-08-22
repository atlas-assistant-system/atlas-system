package atlas.presentation.sharedkernel.sse;

import java.io.IOException;

@FunctionalInterface
interface SseDelivery {

    void to(SseClient client) throws IOException;
}
