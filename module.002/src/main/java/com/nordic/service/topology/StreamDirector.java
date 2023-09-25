package com.nordic.service.topology;



import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutionException;


public class StreamDirector<K, V> extends StrategyContext<KStream<K, V>> {

    private final Properties properties;

    public StreamDirector(Properties properties) {
        super(new ArrayList<>());
        this.properties = properties;
    }

    public StreamDirector<K, V> add(Strategy<KStream<K, V>> strategy) {
        super.strategy().add(strategy);
        return this;
    }

    public void operate() {
        StreamsBuilder builder = new StreamsBuilder();
        super.trigger(builder.stream(properties.getProperty("topic")));

        KafkaStreams streams = new KafkaStreams(builder.build(), properties);
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private boolean attest() {
        try (AdminClient client = AdminClient.create(properties)) {
            return client.listTopics(new ListTopicsOptions()
                    .listInternal(true)).names().get().contains(properties.getProperty("topic"));
        } catch (InterruptedException | ExecutionException ignored) {
            return false;
        }
    }

}
