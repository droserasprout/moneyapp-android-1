package com.cactusteam.money.sync.changes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author vpotapenko
 */
public class ChangesListFactory {

    private final ObjectMapper objectMapper;

    public ChangesListFactory() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public ChangesList read(InputStream inputStream) throws IOException {
        return objectMapper.readValue(inputStream, ChangesList.class);
    }

    public void write(OutputStream outputStream, ChangesList changesList) throws IOException {
        objectMapper.writeValue(outputStream, changesList);
    }

    public void setFormattedOutput(boolean formattedOutput) {
        if (formattedOutput) {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        } else {
            objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        }
    }
}
