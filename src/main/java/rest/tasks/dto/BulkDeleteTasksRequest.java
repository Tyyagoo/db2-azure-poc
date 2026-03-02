package rest.tasks.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

public class BulkDeleteTasksRequest {

    @NotEmpty
    private List<@NotBlank String> ids;

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }
}
