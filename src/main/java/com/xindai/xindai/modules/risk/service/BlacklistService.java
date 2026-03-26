package com.xindai.xindai.modules.risk.service;

import com.xindai.xindai.modules.risk.entity.Blacklist;
import java.util.List;

public interface BlacklistService {
    boolean isPhoneBlacklisted(String phone);
    boolean isIdCardBlacklisted(String idCard);
    void addToBlacklist(Integer type, String value, String reason);
    void removeFromBlacklist(Integer type, String value);
    List<Blacklist> getBlacklist(Integer type);
}
