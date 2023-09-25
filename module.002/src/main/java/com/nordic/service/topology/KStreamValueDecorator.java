package com.nordic.service.topology;


import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.Arrays;

@Slf4j
@NoArgsConstructor
public class KStreamValueDecorator implements Transformer<Object, Object, KeyValue<Object, Object>> {

    private final JsonNode parser = new JsonNode();
    private ProcessorContext context;

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
    }

    @Override
    public KeyValue<Object, Object> transform(Object key, Object value) {
        parser.clear();
        parser.setNode("key", key.toString());
        parser.setNode("message", value.toString());
        Arrays.stream(context.headers().toArray())
                .forEach(header -> parser.setParentNode("header", header.key(), new String(header.value())));
        return new KeyValue<>(key, parser.extract());
    }

    @Override
    public void close() {
    }

}
