package com.xindai.xindai.client.thirdparty.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CarrierData {
    // 基础信息
    private String phone;
    private String carrier;
    private String province;
    private String city;

    // 在网信息
    private Integer networkAge;  // 在网时长(月)
    private BigDecimal networkAgeScore;

    // 通话行为
    private Integer callCount30d;
    private Integer callCount90d;
    private BigDecimal avgCallDuration;
    private Integer nightCallRatio;

    // 位置信息
    private List<LocationRecord> locations;
    private BigDecimal locationStability;

    // 联系人
    private Integer contactCount;
    private Integer frequentContactCount;
    private BigDecimal contactStability;

    @Data
    public static class LocationRecord {
        private String province;
        private String city;
        private Integer days;
    }
}
