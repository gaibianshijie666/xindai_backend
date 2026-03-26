package com.xindai.xindai.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.xindai.xindai.common.annotation.Desensitize;
import com.xindai.xindai.common.util.DesensitizeUtils;

import java.io.IOException;

public class DesensitizeSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private Desensitize.DesensitizeType type;

    public DesensitizeSerializer() {}

    public DesensitizeSerializer(Desensitize.DesensitizeType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        switch (type) {
            case PHONE -> gen.writeString(DesensitizeUtils.maskPhone(value));
            case ID_CARD -> gen.writeString(DesensitizeUtils.maskIdCard(value));
            case NAME -> gen.writeString(DesensitizeUtils.maskName(value));
            default -> gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property != null) {
            Desensitize anno = property.getAnnotation(Desensitize.class);
            if (anno != null) {
                return new DesensitizeSerializer(anno.value());
            }
        }
        return this;
    }
}
