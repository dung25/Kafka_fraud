package com.example.model;

import java.time.Instant;

/**
 * Represents a customer record from the Kafka stream.
 * This model is designed to handle the specific JSON structure provided.
 */
public class CustomerRecord {
    private String recId;
    private String mnemonic;
    private String shortName;
    private String name1;
    private String street;
    private String address;
    private String townCountry;
    private String postCode;
    private String country;
    private String sector;
    private String accountOfficer;
    private String nationality;
    private String customerStatus;
    private String taxId;
    private String emailAddress;
    private String phoneNumber;
    private String mobileNumber;
    private String dateOfBirth;
    private String employmentStatus;
    private String timestamp; // Added for tracking when the record was processed
    private boolean isBlacklisted = false;
    private String blacklistReason;

    // Getters and Setters
    public String getRecId() {
        return recId;
    }

    public void setRecId(String recId) {
        this.recId = recId;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public void setMnemonic(String mnemonic) {
        this.mnemonic = mnemonic;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getName1() {
        return name1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTownCountry() {
        return townCountry;
    }

    public void setTownCountry(String townCountry) {
        this.townCountry = townCountry;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getAccountOfficer() {
        return accountOfficer;
    }

    public void setAccountOfficer(String accountOfficer) {
        this.accountOfficer = accountOfficer;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getCustomerStatus() {
        return customerStatus;
    }

    public void setCustomerStatus(String customerStatus) {
        this.customerStatus = customerStatus;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isBlacklisted() {
        return isBlacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        isBlacklisted = blacklisted;
    }

    public String getBlacklistReason() {
        return blacklistReason;
    }

    public void setBlacklistReason(String blacklistReason) {
        this.blacklistReason = blacklistReason;
    }

    @Override
    public String toString() {
        return "CustomerRecord{" +
                "recId='" + recId + '\'' +
                ", mnemonic='" + mnemonic + '\'' +
                ", shortName='" + shortName + '\'' +
                ", nationality='" + nationality + '\'' +
                ", taxId='" + taxId + '\'' +
                ", isBlacklisted=" + isBlacklisted +
                ", blacklistReason='" + blacklistReason + '\'' +
                '}';
    }
}