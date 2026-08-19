package sharedkernel.infrastructure.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Migrations {

    private static final Pattern FILE_NAME = Pattern.compile("V(\\d+)__([A-Za-z0-9_]+)\\.sql");

    private Migrations() {}

    public static List<Migration> load(Class<?> anchor, String directory, String... fileNames) {
        var loaded = new ArrayList<Migration>(fileNames.length);

        for (var fileName : fileNames) {
            loaded.add(read(anchor, directory, fileName));
        }

        return List.copyOf(loaded);
    }

    private static Migration read(Class<?> anchor, String directory, String fileName) {
        var matcher = FILE_NAME.matcher(fileName);

        if (!matcher.matches()) {
            throw new PersistenceException(
                "Migration file name must look like V001__description.sql but was: " + fileName);
        }

        var path = directory.endsWith("/") ? directory + fileName : directory + "/" + fileName;

        try (InputStream stream = anchor.getResourceAsStream(path)) {
            if (stream == null) {
                throw new PersistenceException("Migration resource not found: " + path);
            }

            var sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            return new Migration(Integer.parseInt(matcher.group(1)), matcher.group(2), sql);
        } catch (IOException e) {
            throw new PersistenceException("Failed to read migration " + path, e);
        }
    }
}
