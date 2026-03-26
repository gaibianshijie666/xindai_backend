package com.xindai.xindai.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.modules.user.dto.UserProfileUpdateDTO;
import com.xindai.xindai.modules.user.dto.UserProfileVO;
import com.xindai.xindai.modules.user.entity.UserProfile;
import com.xindai.xindai.modules.user.mapper.UserProfileMapper;
import com.xindai.xindai.modules.user.service.UserProfileDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileDetailServiceImpl implements UserProfileDetailService {

    private final UserProfileMapper userProfileMapper;

    @Override
    public UserProfile getUserProfileByUserId(Long userId) {
        return userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
        );
    }

    @Override
    public UserProfileVO getProfileDetail(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
        );

        if (profile == null) {
            return null;
        }

        return convertToProfileVO(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateProfileDetail(Long userId, UserProfileUpdateDTO dto) {
        // 获取或创建用户画像
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
        );

        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setCreatedAt(LocalDateTime.now());
        }

        // 更新字段
        if (dto.getAnnualIncome() != null) {
            profile.setAnnualIncome(dto.getAnnualIncome());
        } else if (dto.getMonthlyIncome() != null) {
            // 根据月收入计算年收入
            profile.setAnnualIncome(dto.getMonthlyIncome().multiply(new BigDecimal("12")));
        }

        if (dto.getEmploymentYears() != null) {
            profile.setEmploymentYears(dto.getEmploymentYears());
        }

        if (dto.getMonthlyDebt() != null && profile.getAnnualIncome() != null) {
            // 计算 DTI（债务收入比）
            BigDecimal monthlyIncome = profile.getAnnualIncome().divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dti = dto.getMonthlyDebt().divide(monthlyIncome, 4, RoundingMode.HALF_UP);
                profile.setDti(dti);
            }
        }

        if (dto.getCompany() != null) {
            profile.setCompany(dto.getCompany());
        }

        if (dto.getPosition() != null) {
            profile.setPosition(dto.getPosition());
        }

        profile.setProfileUpdatedAt(LocalDateTime.now());

        if (profile.getId() == null) {
            userProfileMapper.insert(profile);
        } else {
            userProfileMapper.updateById(profile);
        }

        log.info("User profile detail updated: userId={}", userId);
        return convertToProfileVO(profile);
    }

    private UserProfileVO convertToProfileVO(UserProfile profile) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(profile.getId());
        vo.setUserId(profile.getUserId());
        vo.setCreditScore(profile.getCreditScore());
        vo.setRiskLevel(profile.getRiskLevel());
        vo.setCreditGrade(profile.getCreditGrade());
        vo.setAnnualIncome(profile.getAnnualIncome());
        vo.setDti(profile.getDti());
        vo.setEmploymentYears(profile.getEmploymentYears());
        vo.setRiskScore(profile.getRiskScore());
        vo.setCompany(profile.getCompany());
        vo.setPosition(profile.getPosition());
        vo.setProfileUpdatedAt(profile.getProfileUpdatedAt());
        vo.setCreatedAt(profile.getCreatedAt());
        return vo;
    }
}
