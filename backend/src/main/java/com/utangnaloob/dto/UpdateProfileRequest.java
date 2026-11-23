package com.utangnaloob.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 60, message = "First name must not exceed 60 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 60, message = "Last name must not exceed 60 characters")
    private String lastName;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    @Size(max = 30, message = "Date of birth format invalid")
    private String dateOfBirth;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Size(max = 50, message = "Employment status invalid")
    private String employmentStatus;

    @DecimalMin(value = "0.0", message = "Monthly income must be greater than or equal to 0")
    private Double monthlyIncome;

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String firstName, String lastName, String phoneNumber,
                                String dateOfBirth, String address, String employmentStatus,
                                Double monthlyIncome) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.employmentStatus = employmentStatus;
        this.monthlyIncome = monthlyIncome;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public Double getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(Double monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }
}
