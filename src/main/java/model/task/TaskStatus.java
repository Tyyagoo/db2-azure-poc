package model.task;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum TaskStatus {
    OPEN("open"),
    COMPLETED("completed");

    private final String apiValue;

    TaskStatus(String apiValue) {
        this.apiValue = apiValue;
    }

    @JsonValue
    public String apiValue() {
        return apiValue;
    }

    @JsonCreator
    public static TaskStatus fromApiValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.apiValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported task status: " + value));
    }
}

@Converter(autoApply = false)
class TaskStatusConverter implements AttributeConverter<TaskStatus, String> {

    @Override
    public String convertToDatabaseColumn(TaskStatus attribute) {
        return attribute == null ? null : attribute.apiValue();
    }

    @Override
    public TaskStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TaskStatus.fromApiValue(dbData);
    }
}
