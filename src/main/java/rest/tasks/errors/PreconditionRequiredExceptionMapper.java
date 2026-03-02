package rest.tasks.errors;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class PreconditionRequiredExceptionMapper implements ExceptionMapper<PreconditionRequiredException> {

    @Override
    public Response toResponse(PreconditionRequiredException exception) {
        return Response.status(428)
                .type(MediaType.APPLICATION_JSON)
                .entity(ApiErrorResponse.of("precondition_required", exception.getMessage()))
                .build();
    }
}
