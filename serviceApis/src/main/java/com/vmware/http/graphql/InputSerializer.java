package com.vmware.http.graphql;

import com.google.gson.annotations.Expose;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Basic serializer for mutation inputs
 */
public class InputSerializer {
    public String serialize(Object input) {
        Field[] fieldsToCheck = input.getClass().getFields();
        StringBuilder output = new StringBuilder();
        for (Field field : fieldsToCheck) {
            if (field.getType().isPrimitive()) {
                continue;
            }
            Expose exposeAnnotation = field.getAnnotation(Expose.class);
            if (exposeAnnotation != null && !exposeAnnotation.serialize()) {
                continue;
            }
            try {
                Object value = field.get(input);
                if (value != null) {
                    if (output.length() > 0) {
                        output.append(", ");
                    } else {
                        output.append("{ ");
                    }
                    output.append(field.getName()).append(": ").append(getValueAsText(value));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        output.append("}");
        return output.toString();
    }

    private String getValueAsText(Object value) {
        if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Integer || value instanceof Long) {
            return value.toString();
        } else if (value.getClass().isArray()) {
            Object[] values = (Object[]) value;
            return "[" + Arrays.stream(values).map(this::getValueAsText).collect(Collectors.joining(", ")) + "]";
        } else if (value.getClass().isEnum()) {
            return value.toString();
        } else if (value  instanceof String && ((String) value).contains("\n")) {
            return "\"\"\"" + value + "\"\"\"";
        } else {
            return "\"" + value + "\"";
        }
    }
}
