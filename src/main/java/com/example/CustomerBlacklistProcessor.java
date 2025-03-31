package com.example;

import com.example.db.OracleBlacklistRepository;
import com.example.model.CustomerRecord;
import com.example.serialization.CustomerRecordDeserializer;
import com.example.serialization.CustomerRecordSerializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class CustomerBlacklistProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CustomerBlacklistProcessor.class);

    public static void main(String[] args) {
        // Load application properties
        Properties appProps = loadProperties();
        if (appProps == null) {
            System.exit(1);
        }

        // Configure Kafka stream properties
        Properties streamProps = new Properties();
        streamProps.put(StreamsConfig.APPLICATION_ID_CONFIG, "customer-blacklist-processor");
        streamProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                appProps.getProperty("kafka.bootstrap.servers", "10.14.222.194:9092"));
        streamProps.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamProps.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamProps.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);

        // Create a custom Serde for CustomerRecord objects
        final Serde<CustomerRecord> customerSerde = Serdes.serdeFrom(
                new CustomerRecordSerializer(), new CustomerRecordDeserializer());

        // Build the Kafka Streams topology
        final StreamsBuilder builder = new StreamsBuilder();

        // Define input and output topics from properties
        final String inputTopic = appProps.getProperty("kafka.input.topic", "customers");
        final String outputTopic = appProps.getProperty("kafka.output.topic", "verified-customers");
        final String blacklistedTopic = appProps.getProperty("kafka.blacklisted.topic", "blacklisted-customers");

        // Get Oracle connection details from properties
        String jdbcUrl = appProps.getProperty("oracle.jdbc.url");
        String dbUsername = appProps.getProperty("oracle.username");
        String dbPassword = appProps.getProperty("oracle.password");

        logger.info("Using Oracle connection: {}", jdbcUrl);

        try (OracleBlacklistRepository blacklistRepo = new OracleBlacklistRepository(jdbcUrl, dbUsername, dbPassword)) {
            // Define the stream processing topology
            KStream<String, CustomerRecord> customerStream = builder.stream(
                    inputTopic,
                    Consumed.with(Serdes.String(), customerSerde)
            );

            // Process each customer record to check blacklist status
            KStream<String, CustomerRecord>[] branches = customerStream
                    .peek((key, record) -> logger.info("Processing customer: {} ({})",
                            record.getShortName(), record.getRecId()))
                    .mapValues(record -> {
                        // Check if customer is blacklisted based on various attributes
                        Map<String, String> blacklistResult = blacklistRepo.checkBlacklist(
                                record.getRecId(),
                                record.getTaxId(),
                                record.getNationality()
                        );

                        if (!blacklistResult.isEmpty()) {
                            record.setBlacklisted(true);

                            // Combine reasons if there are multiple blacklist matches
                            StringBuilder reason = new StringBuilder();
                            if (blacklistResult.containsKey("customer")) {
                                reason.append("Customer ID: ").append(blacklistResult.get("customer"));
                            }
                            if (blacklistResult.containsKey("taxId")) {
                                if (reason.length() > 0) {
                                    reason.append("; ");
                                }
                                reason.append("Tax ID: ").append(blacklistResult.get("taxId"));
                            }
                            if (blacklistResult.containsKey("nationality")) {
                                if (reason.length() > 0) {
                                    reason.append("; ");
                                }
                                reason.append("Nationality: ").append(blacklistResult.get("nationality"));
                            }

                            record.setBlacklistReason(reason.toString());
                        }

                        return record;
                    })
                    .branch(
                            // First branch: records that are NOT blacklisted
                            (key, record) -> !record.isBlacklisted(),
                            // Second branch: records that ARE blacklisted
                            (key, record) -> record.isBlacklisted()
                    );

            // Send non-blacklisted records to the verified topic
            branches[0]
                    .peek((key, record) -> logger.info("Customer passed verification: {} ({})",
                            record.getShortName(), record.getRecId()))
                    .to(outputTopic, Produced.with(Serdes.String(), customerSerde));

            // Send blacklisted records to the blacklisted topic
            branches[1]
                    .peek((key, record) -> logger.warn("Customer blacklisted: {} ({}), Reason: {}",
                            record.getShortName(), record.getRecId(), record.getBlacklistReason()))
                    .to(blacklistedTopic, Produced.with(Serdes.String(), customerSerde));

            // Create and start the Kafka Streams instance
            final KafkaStreams streams = new KafkaStreams(builder.build(), streamProps);
            final CountDownLatch latch = new CountDownLatch(1);

            // Clean up on shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                streams.close();
                latch.countDown();
            }));

            try {
                streams.start();
                logger.info("Kafka Streams started successfully for customer blacklist processing");
                latch.await();
            } catch (InterruptedException e) {
                logger.error("Stream interrupted: {}", e.getMessage(), e);
                System.exit(1);
            }
        } catch (Exception e) {
            logger.error("Error in customer processing: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Loads application properties from application.properties file
     */
    private static Properties loadProperties() {
        Properties properties = new Properties();

        // Try to load from classpath first
        try (InputStream input = CustomerBlacklistProcessor.class.getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (input == null) {
                // If not found in classpath, try to load from file system
                try (FileInputStream fileInput = new FileInputStream("src/main/resources/application.properties")) {
                    properties.load(fileInput);
                    logger.info("Loaded properties from file system");
                    return properties;
                } catch (IOException e) {
                    logger.error("Cannot find application.properties in classpath or file system: {}", e.getMessage());
                    return null;
                }
            }

            properties.load(input);
            logger.info("Loaded properties from classpath");
            return properties;

        } catch (IOException e) {
            logger.error("Error loading application.properties: {}", e.getMessage());
            return null;
        }
    }
}