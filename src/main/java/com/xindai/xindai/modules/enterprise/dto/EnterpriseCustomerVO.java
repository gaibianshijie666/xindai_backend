package com.xindai.xindai.modules.enterprise.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EnterpriseCustomerVO {
    private Long id;
    private String customerNo;
    private String realName;

    @JsonIgnore
    private String idCard;

    @Schema(description = "身份证号（脱敏）")
    public String getMaskedIdCard() {
        if (idCard == null || idCard.length() < 18) {
            return "***";
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    @JsonIgnore
    private String phone;

    @Schema(description = "手机号（脱敏）")
    public String getMaskedPhone() {
        if (phone == null || phone.length() < 11) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private Integer creditScore;
    private Integer riskLevel;
    private Integer totalLoanCount;
    private BigDecimal totalLoanAmount;
    private Integer status;
    private LocalDateTime createdAt;
}
