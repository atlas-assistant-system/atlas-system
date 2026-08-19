package atlas.infrastructure.presence.persistence;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class FaceDescriptors {

    private FaceDescriptors() {}

    static byte[] encode(float[] values) {
        var buffer = ByteBuffer.allocate(values.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (var value : values) {
            buffer.putFloat(value);
        }

        return buffer.array();
    }

    static float[] decode(byte[] bytes) {
        if (bytes.length == 0 || bytes.length % Float.BYTES != 0) {
            throw new IllegalArgumentException("A face descriptor BLOB must contain one or more complete floats");
        }

        var buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        var values = new float[bytes.length / Float.BYTES];
        for (var index = 0; index < values.length; index++) {
            values[index] = buffer.getFloat();
        }

        return values;
    }
}
