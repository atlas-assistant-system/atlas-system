package atlas.presentation.sharedkernel.http;

final class BodyTooLargeException extends RuntimeException {

    BodyTooLargeException() {
        super("The request body exceeds " + Router.MAX_BODY_BYTES + " bytes.");
    }
}
