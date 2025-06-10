package com.vmware.http.json;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.WorkflowConfigMapper;
import com.vmware.config.jira.IssueTypeDefinition;
import com.vmware.jira.domain.IssueResolutionDefinition;
import com.vmware.jira.domain.IssueStatusDefinition;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * Configure a gson builder with custom type adapters and type adapters.
 */
public class ConfiguredGsonBuilder {

    private final Map<String, String> customFieldNames;
    private GsonBuilder builder;

    public ConfiguredGsonBuilder() {
        this(TimeZone.getDefault(), "yyyy-MM-dd HH:mm:ss", null);
    }

    public ConfiguredGsonBuilder(Map<String, String> customFieldNames) {
        this(TimeZone.getDefault(), "yyyy-MM-dd HH:mm:ss", customFieldNames);
    }

    public ConfiguredGsonBuilder(TimeZone serverTimezone, String dateFormat) {
        this(serverTimezone, dateFormat, null);
    }

    private ConfiguredGsonBuilder(TimeZone timeZone, String dateFormat, Map<String, String> customFieldNames) {
        ImprovedExclusionStrategy serializationExclusionStrategy = new ImprovedExclusionStrategy(true);
        ImprovedExclusionStrategy deserializationExclusionStrategy = new ImprovedExclusionStrategy(false);
        this.builder = new GsonBuilder()
                .addSerializationExclusionStrategy(serializationExclusionStrategy)
                .addDeserializationExclusionStrategy(deserializationExclusionStrategy)
                .registerTypeAdapterFactory(new PostDeserializeTypeAdapterFactory())
                .registerTypeAdapter(Date.class, new DateWithTimezoneMapper(dateFormat, timeZone))
                .registerTypeAdapter(IssueStatusDefinition.class, new ComplexEnumMapper())
                .registerTypeAdapter(IssueResolutionDefinition.class, new ComplexEnumMapper())
                .registerTypeAdapter(IssueTypeDefinition.class, new ComplexEnumMapper())
                .registerTypeAdapter(WorkflowConfig.class, new WorkflowConfigMapper());
        this.customFieldNames = customFieldNames;
    }

    public ConfiguredGsonBuilder setPrettyPrinting() {
        builder.setPrettyPrinting();
        return this;
    }

    public ConfiguredGsonBuilder addDoubleAsIntMapper() {
        builder.registerTypeAdapter(Double.class, new DoubleAsIntMapper());
        return this;
    }

    public ConfiguredGsonBuilder namingStrategy(FieldNamingPolicy namingPolicy) {
        if (customFieldNames != null) {
            builder.setFieldNamingStrategy(new RuntimeFieldNamingStrategy(namingPolicy, customFieldNames));
        } else {
            builder.setFieldNamingStrategy(new AnnotationFieldNamingStrategy(namingPolicy));
        }
        return this;
    }

    public Gson build() {
        return builder.create();
    }
}
