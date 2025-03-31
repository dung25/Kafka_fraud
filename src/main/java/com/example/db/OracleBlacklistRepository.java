package com.example.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class OracleBlacklistRepository implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(OracleBlacklistRepository.class);
    private final HikariDataSource dataSource;

    public OracleBlacklistRepository(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);
        logger.info("Oracle blacklist repository initialized");
    }

    /**
     * Checks if a customer ID is blacklisted
     */
    public boolean isCustomerBlacklisted(String customerId) {
        String sql = "SELECT REASON FROM BLACKLISTED_CUSTOMERS WHERE CUSTOMER_ID = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String reason = rs.getString("REASON");
                    logger.info("Customer {} is blacklisted. Reason: {}", customerId, reason);
                    return true;
                }
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error checking if customer is blacklisted: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if a tax ID is blacklisted
     */
    public boolean isTaxIdBlacklisted(String taxId) {
        if (taxId == null || taxId.isEmpty()) {
            return false;
        }

        String sql = "SELECT REASON FROM BLACKLISTED_TAX_IDS WHERE TAX_ID = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, taxId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String reason = rs.getString("REASON");
                    logger.info("Tax ID {} is blacklisted. Reason: {}", taxId, reason);
                    return true;
                }
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error checking if tax ID is blacklisted: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if a customer's nationality is from a sanctioned country
     */
    public boolean isNationalityBlacklisted(String nationality) {
        if (nationality == null || nationality.isEmpty()) {
            return false;
        }

        String sql = "SELECT REASON FROM BLACKLISTED_COUNTRIES WHERE COUNTRY_CODE = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nationality);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String reason = rs.getString("REASON");
                    logger.info("Nationality {} is blacklisted. Reason: {}", nationality, reason);
                    return true;
                }
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error checking if nationality is blacklisted: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks a customer record against various blacklists
     * @param customerId The customer ID to check
     * @param taxId The tax ID to check
     * @param nationality The nationality to check
     * @return Map containing reasons for blacklisting, if any
     */
    public Map<String, String> checkBlacklist(String customerId, String taxId, String nationality) {
        Map<String, String> results = new HashMap<>();

        // Check customer ID
        if (customerId != null && !customerId.isEmpty()) {
            String customerSql = "SELECT REASON FROM BLACKLISTED_CUSTOMERS WHERE CUSTOMER_ID = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(customerSql)) {
                stmt.setString(1, customerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        results.put("customer", rs.getString("REASON"));
                    }
                }
            } catch (SQLException e) {
                logger.error("Error checking customer blacklist: {}", e.getMessage(), e);
            }
        }

        // Check tax ID
        if (taxId != null && !taxId.isEmpty()) {
            String taxIdSql = "SELECT REASON FROM BLACKLISTED_TAX_IDS WHERE TAX_ID = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(taxIdSql)) {
                stmt.setString(1, taxId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        results.put("taxId", rs.getString("REASON"));
                    }
                }
            } catch (SQLException e) {
                logger.error("Error checking tax ID blacklist: {}", e.getMessage(), e);
            }
        }

        // Check nationality
        if (nationality != null && !nationality.isEmpty()) {
            String nationalitySql = "SELECT REASON FROM BLACKLISTED_COUNTRIES WHERE COUNTRY_CODE = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(nationalitySql)) {
                stmt.setString(1, nationality);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        results.put("nationality", rs.getString("REASON"));
                    }
                }
            } catch (SQLException e) {
                logger.error("Error checking nationality blacklist: {}", e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Inserts a blacklisted customer record into the BLACKLISTED_CUSTOMER_LOG table
     *
     * @param customerId The customer ID
     * @param customerName The customer name
     * @param taxId The tax ID (can be null)
     * @param nationality The nationality (can be null)
     * @param reason The reason for blacklisting
     * @return true if insertion was successful, false otherwise
     */
    public boolean insertBlacklistedCustomer(String customerId, String customerName, String taxId,
                                             String nationality, String reason) {
        String sql = "INSERT INTO BLACKLISTED_CUSTOMER_LOG " +
                "(CUSTOMER_ID, CUSTOMER_NAME, TAX_ID, NATIONALITY, BLACKLIST_REASON, DETECTION_TIME) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customerId);
            stmt.setString(2, customerName);

            // Handle nullable fields
            if (taxId != null && !taxId.isEmpty()) {
                stmt.setString(3, taxId);
            } else {
                stmt.setNull(3, java.sql.Types.VARCHAR);
            }

            if (nationality != null && !nationality.isEmpty()) {
                stmt.setString(4, nationality);
            } else {
                stmt.setNull(4, java.sql.Types.VARCHAR);
            }

            stmt.setString(5, reason);
            stmt.setTimestamp(6, Timestamp.from(Instant.now()));

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error inserting blacklisted customer into database: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
            logger.info("Oracle blacklist repository closed");
        }
    }
}