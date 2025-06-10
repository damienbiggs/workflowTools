package com.vmware.http.graphql;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Basic serializer for mutation inputs
 */
public class InputSerializer {
    public String serialize(Object input) {
        List<Field> fieldsToCheck = Arrays.stream(input.getClass().getFields()).filter(field -> !field.getType().isPrimitive()).collect(Collectors.toList());
        StringBuilder output = new StringBuilder();
        for (Field field : fieldsToCheck) {
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
        } else {
            return "\"" + value.toString() + "\"";
        }
    }
}
