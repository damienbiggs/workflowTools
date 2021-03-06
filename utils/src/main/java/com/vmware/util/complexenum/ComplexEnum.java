package com.vmware.util.complexenum;

/**
 * Represents enums where each enum value references an int or a text value.
 *
 * @param <T> Value that the enum contains
 */
public interface ComplexEnum<T> {

    public static final String UNKNOWN_VALUE_NAME = "UnknownValue";

    public T getValue();
}
