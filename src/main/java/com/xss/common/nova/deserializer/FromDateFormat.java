package com.xss.common.nova.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.xss.common.nova.util.BaseDateUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Date;

public class FromDateFormat extends JsonDeserializer<Date> {
    @Override
    public Date deserialize(JsonParser jp, DeserializationContext dc) throws IOException {
        String date = jp.getText();
        if (StringUtils.isBlank(date)) {
            return null;
        }
        return BaseDateUtils.parseDate(date, "yyyy-MM-dd");
    }
}
