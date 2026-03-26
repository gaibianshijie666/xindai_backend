package com.xindai.xindai.client.thirdparty;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "third-party")
public class ThirdPartyConfig {

    // 征信服务配置
    private CreditConfig credit = new CreditConfig();

    // 芝麻信用配置
    private ZhimaConfig zhima = new ZhimaConfig();

    // 运营商配置
    private CarrierConfig carrier = new CarrierConfig();

    // 社交数据配置
    private SocialConfig social = new SocialConfig();

    @Data
    public static class CreditConfig {
        private String url;
        private String appId;
        private String appSecret;
        private int timeout = 5000;
        private boolean enabled = true;
    }

    @Data
    public static class ZhimaConfig {
        private String url;
        private String appId;
        private String privateKey;
        private String publicKey;
        private int timeout = 5000;
        private boolean enabled = true;
    }

    @Data
    public static class CarrierConfig {
        private String url;
        private String appId;
        private String appSecret;
        private int timeout = 10000;
        private boolean enabled = true;
    }

    @Data
    public static class SocialConfig {
        private String url;
        private String appId;
        private String appSecret;
        private int timeout = 5000;
        private boolean enabled = true;
    }
}
