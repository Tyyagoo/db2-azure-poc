package rest.tasks.errors;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class PreconditionFailedExceptionMapper implements ExceptionMapper<PreconditionFailedException> {

    @Override
    public Response toResponse(PreconditionFailedException exception) {
        return Response.status(Response.Status.PRECONDITION_FAILED)
                .type(MediaType.APPLICATION_JSON)
                .entity(ApiErrorResponse.of("precondition_failed", exception.getMessage()))
                .build();
    }
}
