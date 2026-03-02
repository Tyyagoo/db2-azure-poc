package rest.tasks.errors;

public class PreconditionRequiredException extends RuntimeException {

    public PreconditionRequiredException(String message) {
        super(message);
    }
}
