package com.xindai.xindai.client.thirdparty;

import com.xindai.xindai.client.thirdparty.dto.ThirdPartyData;

/**
 * 第三方数据采集接口
 */
public interface DataCollector {

    /**
     * 获取数据源类型
     */
    String getSourceType();

    /**
     * 采集数据
     * @param userId 用户ID
     * @param phone 手机号
     * @param idCard 身份证号
     * @return 采集结果
     */
    ThirdPartyData collect(Long userId, String phone, String idCard);

    /**
     * 异步采集数据
     */
    default java.util.concurrent.CompletableFuture<ThirdPartyData> collectAsync(
            Long userId, String phone, String idCard) {
        return java.util.concurrent.CompletableFuture.supplyAsync(
                () -> collect(userId, phone, idCard)
        );
    }

    /**
     * 检查服务是否可用
     */
    boolean isAvailable();
}
