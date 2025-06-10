package com.vmware.http.json;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;

import java.lang.reflect.Field;

public class AnnotationFieldNamingStrategy implements FieldNamingStrategy {

    protected final FieldNamingPolicy defaultPolicy;

    public AnnotationFieldNamingStrategy(FieldNamingPolicy defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }


    public String translateName(Field field) {
        Class<?> declaringClass = field.getDeclaringClass();

        GsonNamingPolicy fieldNamingPolicy = field.getAnnotation(GsonNamingPolicy.class);
        GsonNamingPolicy classNamingPolicy = declaringClass.getAnnotation(GsonNamingPolicy.class);

        FieldNamingPolicy policy = defaultPolicy;

        if (fieldNamingPolicy != null) {
            policy = fieldNamingPolicy.value();
        } else if (classNamingPolicy != null) {
            policy = classNamingPolicy.value();
        }

        return policy.translateName(field);
    }
}
