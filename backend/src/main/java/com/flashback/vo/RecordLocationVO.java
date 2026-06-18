package com.flashback.vo;

import com.flashback.domain.RecordLocationSource;

import java.math.BigDecimal;

/**
 * 记录位置视图。
 */
public class RecordLocationVO {

    private RecordLocationSource source;
    private String name;
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
