package sharedkernel.infrastructure.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import sharedkernel.domain.guards.NumberGuard;
import sharedkernel.domain.guards.StringGuard;

public record Migration(int version, String name, String sql) {

    public Migration {
        NumberGuard.positive(version, "version");
        StringGuard.notBlank(name, "name");
        StringGuard.notBlank(sql, "sql");
    }

    public String checksum() {
        try {
            var digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(digest.digest(sql.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new PersistenceException("SHA-256 is not available in this JVM", e);
        }
    }
}
