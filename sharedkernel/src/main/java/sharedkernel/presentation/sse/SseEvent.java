package sharedkernel.presentation.sse;

import sharedkernel.domain.exceptions.GuardException;
import sharedkernel.domain.guards.StringGuard;

public record SseEvent(String id, String name, String data) {

    public SseEvent {
        StringGuard.notBlank(name, "name");
        rejectLineBreaks(name, "name");
        rejectLineBreaks(id, "id");

        if (data == null) {
            data = "";
        }
    }

    public static SseEvent named(String name, String data) {
        return new SseEvent(null, name, data);
    }

    public SseEvent withId(String id) {
        return new SseEvent(id, name, data);
    }

    public String toWireFormat() {
        var frame = new StringBuilder();

        if (id != null) {
            frame.append("id: ").append(id).append('\n');
        }

        frame.append("event: ").append(name).append('\n');

        for (var line : normalized(data).split("\n", -1)) {
            frame.append("data: ").append(line).append('\n');
        }

        return frame.append('\n').toString();
    }

    private static String normalized(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static void rejectLineBreaks(String value, String parameterName) {
        if (value != null && (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0)) {
            throw GuardException.forParameter(parameterName, "cannot contain line breaks");
        }
    }
}
