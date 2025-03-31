package com.example.ui;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsContext;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FraudAlertDashboard {
    private static final Logger logger = LoggerFactory.getLogger(FraudAlertDashboard.class);
    private static final int PORT = 7070;
    private static final String WS_PATH = "/ws/alerts";

    // Store connected WebSocket clients
    private static final Set<WsContext> clients = ConcurrentHashMap.newKeySet();

    // Queue to store alerts temporarily
    private static final Queue<String> alertQueue = new ConcurrentLinkedQueue<>();

    // Scheduled executor for checking Kafka continuously
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        // Load application properties
        Properties appProps = loadProperties();
        if (appProps == null) {
            System.exit(1);
        }

        String bootstrapServers = appProps.getProperty("kafka.bootstrap.servers", "localhost:9092");
        String blacklistedTopic = appProps.getProperty("kafka.blacklisted.topic", "blacklisted-transactions");

        // Start Kafka consumer thread
        startKafkaConsumer(bootstrapServers, blacklistedTopic);

        // Start Javalin web server
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
        }).start(PORT);

        // Configure WebSocket endpoint
        app.ws(WS_PATH, ws -> {
            ws.onConnect(ctx -> {
                // Use session ID hashcode instead of getId()
                logger.info("New client connected: {}", ctx.session.hashCode());
                clients.add(ctx);
            });

            ws.onClose(ctx -> {
                // Use session ID hashcode instead of getId()
                logger.info("Client disconnected: {}", ctx.session.hashCode());
                clients.remove(ctx);
            });

            ws.onError(ctx -> {
                logger.error("WebSocket error: {}", ctx.error().getMessage());
                clients.remove(ctx);
            });
        });

        // Start scheduled task to send queued alerts to clients
        scheduler.scheduleAtFixedRate(() -> {
            if (!alertQueue.isEmpty() && !clients.isEmpty()) {
                String alert = alertQueue.poll();
                if (alert != null) {
                    broadcastAlert(alert);
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        logger.info("Fraud Alert Dashboard running at http://localhost:{}", PORT);
    }

    private static void startKafkaConsumer(String bootstrapServers, String topic) {
        Thread consumerThread = new Thread(() -> {
            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "fraud-alert-ui");
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                consumer.subscribe(Collections.singletonList(topic));
                logger.info("Kafka consumer started. Listening to topic: {}", topic);

                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        logger.info("Received alert: {}", record.value());
                        alertQueue.add(record.value());
                    }
                }
            } catch (Exception e) {
                logger.error("Error in Kafka consumer", e);
            }
        });

        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    private static void broadcastAlert(String alert) {
        clients.forEach(client -> {
            try {
                client.send(alert);
            } catch (Exception e) {
                logger.error("Error sending alert to client: {}", e.getMessage());
            }
        });
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        // Try to load from classpath first
        try (InputStream input = FraudAlertDashboard.class.getClassLoader()
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