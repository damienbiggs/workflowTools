package com.vmware.http.json;

import com.google.gson.FieldNamingPolicy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface GsonNamingPolicy {
    FieldNamingPolicy value();
}
