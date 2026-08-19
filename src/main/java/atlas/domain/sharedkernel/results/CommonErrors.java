package atlas.domain.sharedkernel.results;

public final class CommonErrors {

    private CommonErrors() {}

    public static Error notFound(String entityName, Object id) {
        return Error.notFound(
            entityName + ".NotFound",
            entityName + " with ID '" + id + "' was not found.");
    }

    public static Error required(String fieldName) {
        return Error.validation(
            "General.ValueIsRequired",
            "'" + fieldName + "' is required.");
    }

    public static Error invalid(String fieldName) {
        return Error.validation(
            "General.InvalidValue",
            "'" + fieldName + "' has an invalid value.");
    }

    public static Error unexpected(String message) {
        return Error.unexpected("General.Unexpected", message);
    }
}
