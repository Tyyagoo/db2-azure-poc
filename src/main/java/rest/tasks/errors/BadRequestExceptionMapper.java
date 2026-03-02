package rest.tasks.errors;

import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException> {

    @Override
    public Response toResponse(BadRequestException exception) {
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? "Invalid request"
                : exception.getMessage();

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(ApiErrorResponse.of("bad_request", message, Map.of()))
                .build();
    }
}
