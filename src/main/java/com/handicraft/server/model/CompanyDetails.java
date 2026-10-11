package com.handicraft.server.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("CompanyDetails")
public class CompanyDetails {
    @Id
    private String id;

    private String companyName;
    private String companyOwnerName;
    private String companyAddress;
    private String state;
    private String zipCode;
    private String gstNumber;
    private String contactNumber;
    private String emailAddress;
    private String managerName;
    private String managerContactNumber;
    private String managerEmailAddress;

    public CompanyDetails() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyOwnerName() {
        return companyOwnerName;
    }

    public void setCompanyOwnerName(String companyOwnerName) {
        this.companyOwnerName = companyOwnerName;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getManagerContactNumber() {
        return managerContactNumber;
    }

    public void setManagerContactNumber(String managerContactNumber) {
        this.managerContactNumber = managerContactNumber;
    }

    public String getManagerEmailAddress() {
        return managerEmailAddress;
    }

    public void setManagerEmailAddress(String managerEmailAddress) {
        this.managerEmailAddress = managerEmailAddress;
    }
}
