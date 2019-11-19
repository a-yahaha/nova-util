package com.xss.common.nova.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.xss.common.nova.annotation.JsonMosaic;
import com.xss.common.nova.util.BaseJsonUtils;
import com.xss.common.nova.util.BaseStringUtils;

import java.io.IOException;

public class CustomSerializer extends StdSerializer {
    private JsonMosaic jsonMosaic;

    public CustomSerializer(JsonMosaic jsonMosaic) {
        super((Class) null);
        this.jsonMosaic = jsonMosaic;
    }

    @Override
    public void serialize(Object obj, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (obj == null) {
            return;
        }

        String text = BaseJsonUtils.writeValue(obj);
        text = BaseStringUtils.mosaic(text, jsonMosaic, '*');
        jsonGenerator.writeString(text);
    }
}
