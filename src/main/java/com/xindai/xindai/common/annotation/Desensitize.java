package com.xindai.xindai.common.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.xindai.xindai.common.serializer.DesensitizeSerializer;

import java.lang.annotation.*;

@JacksonAnnotationsInside
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JsonSerialize(using = DesensitizeSerializer.class)
public @interface Desensitize {
    DesensitizeType value();

    enum DesensitizeType {
        PHONE,
        ID_CARD,
        NAME
    }
}
