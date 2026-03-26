package com.xindai.xindai.modules.risk.feature;

import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.entity.UserProfile;
import java.util.Map;

/**
 * 特征提取器接口
 */
public interface FeatureExtractor {

    /**
     * 获取特征组名称
     */
    String getFeatureGroup();

    /**
     * 提取特征
     * @param userId 用户ID
     * @param user 用户实体
     * @param profile 用户画像
     * @return 特征Map
     */
    Map<String, Float> extract(Long userId, User user, UserProfile profile);

    /**
     * 获取执行顺序（数字越小越先执行）
     */
    int getOrder();
}
