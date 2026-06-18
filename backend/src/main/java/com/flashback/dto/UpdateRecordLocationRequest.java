package com.flashback.dto;

import com.flashback.domain.RecordLocationSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * M4 record location update request.
 */
public class UpdateRecordLocationRequest {

    @NotNull(message = "source不能为空")
    private RecordLocationSource source;

    @Size(max = 100, message = "name长度不能超过100")
    private String name;

    @Size(max = 255, message = "address长度不能超过255")
    private String address;

    private BigDecimal latitude;
    private BigDecimal longitude;

    public RecordLocationSource getSource() {
        return source;
    }

    public void setSource(RecordLocationSource source) {
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }
}
