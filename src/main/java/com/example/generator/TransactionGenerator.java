package com.example.generator;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TransactionGenerator - A utility class that generates random transactions and sends them to Kafka
 * for testing the transaction blacklist processor.
 */
public class TransactionGenerator {
    private static final Logger logger = LoggerFactory.getLogger(TransactionGenerator.class);

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "transactions";

    // Sample data for realistic transaction generation
    private static final String[] ACCOUNT_IDS = {
            "ACC123456", // Blacklisted account from our test data
            "ACC234567",
            "ACC345678",
            "ACC456789",
            "ACC567890",
            "ACC678901",
            "ACC789012",
            "ACC890123",
            "ACC901234"
    };

    private static final String[] COUNTERPARTY_IDS = {
            "CP789012", // Blacklisted counterparty from our test data
            "CP123456",
            "CP234567",
            "CP345678",
            "CP456789",
            "CP567890",
            "CP678901",
            "CP890123",
            "CP901234"
    };

    private static final String[] CURRENCIES = {"USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF"};

    private final KafkaProducer<String, String> producer;
    private final Random random = new Random();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public TransactionGenerator() {
        // Configure Kafka producer
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");

        this.producer = new KafkaProducer<>(props);

        logger.info("Transaction generator initialized with bootstrap servers: {}", BOOTSTRAP_SERVERS);
    }

    /**
     * Generates a random transaction and returns it as a JSON string
     */
    private String generateRandomTransaction() {
        String transactionId = "TX" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String accountId = ACCOUNT_IDS[random.nextInt(ACCOUNT_IDS.length)];
        String counterpartyId = COUNTERPARTY_IDS[random.nextInt(COUNTERPARTY_IDS.length)];
        double amount = 100 + (10000 - 100) * random.nextDouble(); // Random amount between 100 and 10000
        String currency = CURRENCIES[random.nextInt(CURRENCIES.length)];
        String timestamp = Instant.now().toString();

        JSONObject transaction = new JSONObject();
        transaction.put("transactionId", transactionId);
        transaction.put("accountId", accountId);
        transaction.put("counterpartyId", counterpartyId);
        transaction.put("amount", Math.round(amount * 100.0) / 100.0); // Round to 2 decimal places
        transaction.put("currency", currency);
        transaction.put("timestamp", timestamp);

        return transaction.toString();
    }

    /**
     * Sends a single random transaction to Kafka
     */
    public void sendRandomTransaction() {
        String transaction = generateRandomTransaction();
        String key = UUID.randomUUID().toString();

        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, transaction);

        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                logger.info("Transaction sent: {} - offset: {}", transaction, metadata.offset());
            } else {
                logger.error("Error sending transaction: {}", exception.getMessage(), exception);
            }
        });
    }

    /**
     * Starts sending transactions at a fixed rate
     *
     * @param ratePerSecond Number of transactions to send per second
     */
    public void startSending(int ratePerSecond) {
        long periodMs = 1000 / ratePerSecond;

        logger.info("Starting to send transactions at a rate of {} per second", ratePerSecond);

        executor.scheduleAtFixedRate(this::sendRandomTransaction, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops sending transactions
     */
    public void stopSending() {
        logger.info("Stopping transaction generator");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        producer.close();
        logger.info("Transaction generator stopped");
    }

    /**
     * Sends a specific number of transactions immediately in a batch
     *
     * @param count Number of transactions to send
     */
    public void sendBatch(int count) {
        logger.info("Sending batch of {} transactions", count);

        for (int i = 0; i < count; i++) {
            sendRandomTransaction();

            // Small delay to avoid overwhelming the producer
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.info("Batch completed - {} transactions sent", count);
    }

    /**
     * Main method to run the generator
     */
    public static void main(String[] args) {
        TransactionGenerator generator = new TransactionGenerator();

        // Parse command-line arguments
        if (args.length > 0) {
            String command = args[0].toLowerCase();

            switch (command) {
                case "continuous":
                    int rate = args.length > 1 ? Integer.parseInt(args[1]) : 1;
                    int duration = args.length > 2 ? Integer.parseInt(args[2]) : 60;

                    generator.startSending(rate);

                    // Run for the specified duration (in seconds)
                    try {
                        Thread.sleep(duration * 1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        generator.stopSending();
                    }
                    break;

                case "batch":
                    int count = args.length > 1 ? Integer.parseInt(args[1]) : 10;
                    generator.sendBatch(count);
                    generator.producer.close();
                    break;

                default:
                    System.out.println("Unknown command: " + command);
                    printUsage();
                    break;
            }
        } else {
            // Default: send 10 transactions in batch
            generator.sendBatch(10);
            generator.producer.close();
        }
    }

    /**
     * Prints usage instructions for the command-line
     */
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java TransactionGenerator [command] [arguments]");
        System.out.println("");
        System.out.println("Commands:");
        System.out.println("  batch [count]           - Send a batch of [count] transactions (default: 10)");
        System.out.println("  continuous [rate] [sec] - Send transactions continuously at [rate] per second (default: 1)");
        System.out.println("                            for [sec] seconds (default: 60)");
        System.out.println("");
        System.out.println("Examples:");
        System.out.println("  java TransactionGenerator                   - Send 10 transactions in a batch");
        System.out.println("  java TransactionGenerator batch 100         - Send 100 transactions in a batch");
        System.out.println("  java TransactionGenerator continuous 5 120  - Send 5 transactions per second for 2 minutes");
    }
}