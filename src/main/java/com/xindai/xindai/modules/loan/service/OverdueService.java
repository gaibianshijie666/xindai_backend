package com.xindai.xindai.modules.loan.service;

import com.xindai.xindai.modules.loan.dto.OverdueInfoVO;
import java.util.List;

public interface OverdueService {
    void checkAndMarkOverdue();
    List<OverdueInfoVO> getOverdueList(Long userId);
}
