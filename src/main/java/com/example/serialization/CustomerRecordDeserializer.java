package com.example.serialization;

import com.example.model.CustomerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class CustomerRecordDeserializer implements Deserializer<CustomerRecord> {
    private static final Logger logger = LoggerFactory.getLogger(CustomerRecordDeserializer.class);

    @Override
    public CustomerRecord deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            String json = new String(data, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(json);

            // Extract payload object which contains the actual data
            JSONObject payload = jsonObject.getJSONObject("payload");

            CustomerRecord record = new CustomerRecord();

            // Set fields from payload
            record.setRecId(getStringOrNull(payload, "RECID"));
            record.setMnemonic(getStringOrNull(payload, "MNEMONIC"));
            record.setShortName(getStringOrNull(payload, "SHORT_NAME"));
            record.setName1(getStringOrNull(payload, "NAME_1"));
            record.setStreet(getStringOrNull(payload, "STREET"));
            record.setAddress(getStringOrNull(payload, "ADDRESS"));
            record.setTownCountry(getStringOrNull(payload, "TOWN_COUNTRY"));
            record.setPostCode(getStringOrNull(payload, "POST_CODE"));
            record.setCountry(getStringOrNull(payload, "COUNTRY"));
            record.setSector(getStringOrNull(payload, "SECTOR"));
            record.setAccountOfficer(getStringOrNull(payload, "ACCOUNT_OFFICER"));
            record.setNationality(getStringOrNull(payload, "NATIONALITY"));
            record.setCustomerStatus(getStringOrNull(payload, "CUSTOMER_STATUS"));
            record.setTaxId(getStringOrNull(payload, "TAX_ID"));
            record.setEmailAddress(getStringOrNull(payload, "EMAIL_1"));
            record.setPhoneNumber(getStringOrNull(payload, "PHONE_1"));
            record.setMobileNumber(getStringOrNull(payload, "SMS_1"));
            record.setDateOfBirth(getStringOrNull(payload, "DATE_OF_BIRTH"));
            record.setEmploymentStatus(getStringOrNull(payload, "EMPLOYMENT_STATUS"));

            // Set timestamp from DATE_TIME or current time
            String dateTime = getStringOrNull(payload, "DATE_TIME");
            if (dateTime != null) {
                record.setTimestamp(dateTime);
            } else {
                record.setTimestamp(Instant.now().toString());
            }

            return record;
        } catch (Exception e) {
            logger.error("Error deserializing customer record: {}", e.getMessage(), e);
            return null;
        }
    }

    private String getStringOrNull(JSONObject json, String key) {
        return json.has(key) && !json.isNull(key) ? json.getString(key) : null;
    }
}