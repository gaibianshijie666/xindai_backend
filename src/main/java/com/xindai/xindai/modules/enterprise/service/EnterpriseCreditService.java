package com.xindai.xindai.modules.enterprise.service;

import com.xindai.xindai.modules.enterprise.dto.CreditApplyDTO;
import com.xindai.xindai.modules.enterprise.dto.CreditInfoVO;

public interface EnterpriseCreditService {
    CreditInfoVO getCreditInfo(Long enterpriseId);
    void applyCreditIncrease(Long enterpriseId, CreditApplyDTO dto);
}
