package rest.tasks.errors;

import java.util.Map;

public record ApiErrorResponse(String error, String message, Map<String, Object> details) {

    public static ApiErrorResponse of(String error, String message) {
        return new ApiErrorResponse(error, message, Map.of());
    }

    public static ApiErrorResponse of(String error, String message, Map<String, Object> details) {
        return new ApiErrorResponse(error, message, details == null ? Map.of() : details);
    }
}
