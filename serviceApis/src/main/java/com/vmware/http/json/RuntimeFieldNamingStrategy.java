package com.vmware.http.json;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;

import java.lang.reflect.Field;
import java.util.Map;


public class RuntimeFieldNamingStrategy extends AnnotationFieldNamingStrategy {

    private final Map<String, String> runtimeFieldNameMappings;

    public RuntimeFieldNamingStrategy(FieldNamingPolicy defaultPolicy, Map<String, String> runtimeFieldNameMappings) {
        super(defaultPolicy);
        this.runtimeFieldNameMappings = runtimeFieldNameMappings;
    }

    @Override
    public String translateName(Field field) {
        RuntimeFieldName runtimeFieldName = field.getAnnotation(RuntimeFieldName.class);
        if (runtimeFieldName == null) {
            return defaultPolicy.translateName(field);
        }
        String fieldVariableName = runtimeFieldName.value();
        if (!runtimeFieldNameMappings.containsKey(fieldVariableName)) {
            throw new RuntimeException("No field name mapping for variable " + field.getName());
        }
        return runtimeFieldNameMappings.get(fieldVariableName);
    }

    public void updateRuntimeFieldNameMapping(String fieldName, String value) {
        runtimeFieldNameMappings.put(fieldName, value);
    }
}
