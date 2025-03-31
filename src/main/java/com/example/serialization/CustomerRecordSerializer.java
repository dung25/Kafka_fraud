package com.example.serialization;

import com.example.model.CustomerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class CustomerRecordSerializer implements Serializer<CustomerRecord> {
    private static final Logger logger = LoggerFactory.getLogger(CustomerRecordSerializer.class);

    @Override
    public byte[] serialize(String topic, CustomerRecord record) {
        if (record == null) {
            return null;
        }

        try {
            JSONObject payload = new JSONObject();

            // Only add non-null fields to the payload
            addIfNotNull(payload, "RECID", record.getRecId());
            addIfNotNull(payload, "MNEMONIC", record.getMnemonic());
            addIfNotNull(payload, "SHORT_NAME", record.getShortName());
            addIfNotNull(payload, "NAME_1", record.getName1());
            addIfNotNull(payload, "STREET", record.getStreet());
            addIfNotNull(payload, "ADDRESS", record.getAddress());
            addIfNotNull(payload, "TOWN_COUNTRY", record.getTownCountry());
            addIfNotNull(payload, "POST_CODE", record.getPostCode());
            addIfNotNull(payload, "COUNTRY", record.getCountry());
            addIfNotNull(payload, "SECTOR", record.getSector());
            addIfNotNull(payload, "ACCOUNT_OFFICER", record.getAccountOfficer());
            addIfNotNull(payload, "NATIONALITY", record.getNationality());
            addIfNotNull(payload, "CUSTOMER_STATUS", record.getCustomerStatus());
            addIfNotNull(payload, "TAX_ID", record.getTaxId());
            addIfNotNull(payload, "EMAIL_1", record.getEmailAddress());
            addIfNotNull(payload, "PHONE_1", record.getPhoneNumber());
            addIfNotNull(payload, "SMS_1", record.getMobileNumber());
            addIfNotNull(payload, "DATE_OF_BIRTH", record.getDateOfBirth());
            addIfNotNull(payload, "EMPLOYMENT_STATUS", record.getEmploymentStatus());
            addIfNotNull(payload, "DATE_TIME", record.getTimestamp());

            // Add blacklist information to a special field
            payload.put("isBlacklisted", record.isBlacklisted());
            if (record.getBlacklistReason() != null) {
                payload.put("blacklistReason", record.getBlacklistReason());
            }

            // Create the schema object (simplified for this example)
            JSONObject schema = new JSONObject();
            schema.put("type", "struct");
            schema.put("name", "SBILH_TEST.Value");

            // Create the full response object
            JSONObject result = new JSONObject();
            result.put("schema", schema);
            result.put("payload", payload);

            return result.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error serializing customer record: {}", e.getMessage(), e);
            return null;
        }
    }

    private void addIfNotNull(JSONObject json, String key, String value) {
        if (value != null) {
            json.put(key, value);
        }
    }
}