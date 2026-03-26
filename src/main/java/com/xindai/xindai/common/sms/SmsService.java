package com.xindai.xindai.common.sms;

import java.util.Map;

public interface SmsService {

    /**
     * Send an SMS message.
     *
     * @param phone       recipient phone number
     * @param templateCode SMS template code
     * @param params      template parameters
     * @return send result
     */
    SendResult send(String phone, String templateCode, Map<String, String> params);
}
