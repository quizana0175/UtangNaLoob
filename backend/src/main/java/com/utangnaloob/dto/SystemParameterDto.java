package com.utangnaloob.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class SystemParameterDto {

    private Long id;

    @NotBlank(message = "Parameter key is required")
    private String paramKey;

    @NotBlank(message = "Parameter value is required")
    private String paramValue;

    private String description;
    private String updatedBy;
    private LocalDateTime updatedAt;

    public SystemParameterDto() {
    }

    public SystemParameterDto(Long id, String paramKey, String paramValue, String description, String updatedBy, LocalDateTime updatedAt) {
        this.id = id;
        this.paramKey = paramKey;
        this.paramValue = paramValue;
        this.description = description;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getParamKey() {
        return paramKey;
    }

    public void setParamKey(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String paramValue) {
        this.paramValue = paramValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
