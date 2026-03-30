package com.xindai.xindai.modules.report.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.loan.entity.CreditLimit;
import com.xindai.xindai.modules.loan.mapper.CreditLimitMapper;
import com.xindai.xindai.modules.report.dto.CreditUsageExcelVO;
import com.xindai.xindai.modules.report.dto.UserGrowthExcelVO;
import com.xindai.xindai.modules.report.service.AdminReportService;
import com.xindai.xindai.modules.report.vo.CreditUsageVO;
import com.xindai.xindai.modules.report.vo.UserGrowthVO;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin report service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private final UserMapper userMapper;
    private final CreditLimitMapper creditLimitMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Page<UserGrowthVO> getUserGrowth(Integer page, Integer size, String startDate, String endDate) {
        LocalDateTime start = startDate != null ? LocalDate.parse(startDate, DATE_FORMATTER).atStartOfDay() : null;
        LocalDateTime end = endDate != null ? LocalDate.parse(endDate, DATE_FORMATTER).plusDays(1).atStartOfDay() : null;

        // Get all users (filtered by date range if provided)
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (start != null) {
            wrapper.ge(User::getCreatedAt, start);
        }
        if (end != null) {
            wrapper.lt(User::getCreatedAt, end);
        }
        wrapper.orderByAsc(User::getCreatedAt);

        List<User> users = userMapper.selectList(wrapper);

        // Group by date and calculate cumulative total
        List<UserGrowthVO> result = new ArrayList<>();
        LocalDate currentDate = null;
        Long dailyCount = 0L;
        Long cumulativeCount = 0L;

        for (User user : users) {
            LocalDate userDate = user.getCreatedAt().toLocalDate();

            if (currentDate == null || !userDate.equals(currentDate)) {
                if (currentDate != null) {
                    UserGrowthVO vo = new UserGrowthVO();
                    vo.setDate(currentDate);
                    vo.setNewUsers(dailyCount);
                    vo.setTotalUsers(cumulativeCount);
                    result.add(vo);
                }
                currentDate = userDate;
                dailyCount = 1L;
            } else {
                dailyCount++;
            }
            cumulativeCount++;
        }

        // Add last entry
        if (currentDate != null) {
            UserGrowthVO vo = new UserGrowthVO();
            vo.setDate(currentDate);
            vo.setNewUsers(dailyCount);
            vo.setTotalUsers(cumulativeCount);
            result.add(vo);
        }

        // Apply pagination
        int pageNum = page != null ? page : 1;
        int pageSize = size != null ? size : 10;
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, result.size());

        Page<UserGrowthVO> pageResult = new Page<>(pageNum, pageSize, result.size());
        if (fromIndex < result.size()) {
            pageResult.setRecords(result.subList(fromIndex, toIndex));
        } else {
            pageResult.setRecords(new ArrayList<>());
        }

        return pageResult;
    }

    @Override
    public void exportUserGrowth(HttpServletResponse response, String startDate, String endDate) {
        LocalDateTime start = startDate != null ? LocalDate.parse(startDate, DATE_FORMATTER).atStartOfDay() : null;
        LocalDateTime end = endDate != null ? LocalDate.parse(endDate, DATE_FORMATTER).plusDays(1).atStartOfDay() : null;

        // Get all users (filtered by date range if provided)
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (start != null) {
            wrapper.ge(User::getCreatedAt, start);
        }
        if (end != null) {
            wrapper.lt(User::getCreatedAt, end);
        }
        wrapper.orderByAsc(User::getCreatedAt);

        List<User> users = userMapper.selectList(wrapper);

        // Group by date and calculate cumulative total
        List<UserGrowthExcelVO> excelList = new ArrayList<>();
        LocalDate currentDate = null;
        Long dailyCount = 0L;
        Long cumulativeCount = 0L;

        for (User user : users) {
            LocalDate userDate = user.getCreatedAt().toLocalDate();

            if (currentDate == null || !userDate.equals(currentDate)) {
                if (currentDate != null) {
                    UserGrowthExcelVO vo = new UserGrowthExcelVO();
                    vo.setDate(currentDate.format(DATE_FORMATTER));
                    vo.setNewUsers(dailyCount);
                    vo.setTotalUsers(cumulativeCount);
                    excelList.add(vo);
                }
                currentDate = userDate;
                dailyCount = 1L;
            } else {
                dailyCount++;
            }
            cumulativeCount++;
        }

        // Add last entry
        if (currentDate != null) {
            UserGrowthExcelVO vo = new UserGrowthExcelVO();
            vo.setDate(currentDate.format(DATE_FORMATTER));
            vo.setNewUsers(dailyCount);
            vo.setTotalUsers(cumulativeCount);
            excelList.add(vo);
        }

        writeExcel(response, "用户增长统计", UserGrowthExcelVO.class, excelList);
    }

    @Override
    public CreditUsageVO getCreditUsage() {
        List<CreditLimit> creditLimits = creditLimitMapper.selectList(
                new LambdaQueryWrapper<CreditLimit>().eq(CreditLimit::getStatus, 1)
        );

        BigDecimal totalLimit = BigDecimal.ZERO;
        BigDecimal usedLimit = BigDecimal.ZERO;

        for (CreditLimit cl : creditLimits) {
            if (cl.getTotalLimit() != null) {
                totalLimit = totalLimit.add(cl.getTotalLimit());
            }
            if (cl.getUsedLimit() != null) {
                usedLimit = usedLimit.add(cl.getUsedLimit());
            }
        }

        BigDecimal availableLimit = totalLimit.subtract(usedLimit);
        BigDecimal utilizationRate = BigDecimal.ZERO;

        if (totalLimit.compareTo(BigDecimal.ZERO) > 0) {
            utilizationRate = usedLimit.divide(totalLimit, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        CreditUsageVO vo = new CreditUsageVO();
        vo.setTotalLimit(totalLimit);
        vo.setUsedLimit(usedLimit);
        vo.setAvailableLimit(availableLimit);
        vo.setUtilizationRate(utilizationRate);
        vo.setActiveUsers((long) creditLimits.size());

        return vo;
    }

    @Override
    public void exportCreditUsage(HttpServletResponse response) {
        CreditUsageVO vo = getCreditUsage();

        CreditUsageExcelVO excelVO = new CreditUsageExcelVO();
        excelVO.setTotalLimit(vo.getTotalLimit());
        excelVO.setUsedLimit(vo.getUsedLimit());
        excelVO.setAvailableLimit(vo.getAvailableLimit());
        excelVO.setUtilizationRate(vo.getUtilizationRate());
        excelVO.setActiveUsers(vo.getActiveUsers());

        List<CreditUsageExcelVO> excelList = List.of(excelVO);
        writeExcel(response, "额度使用统计", CreditUsageExcelVO.class, excelList);
    }

    private <T> void writeExcel(HttpServletResponse response, String fileName, Class<T> clazz, List<T> data) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");

        try {
            EasyExcel.write(response.getOutputStream(), clazz)
                    .sheet(fileName)
                    .doWrite(data);
        } catch (IOException e) {
            log.error("Failed to export Excel: {}", fileName, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出Excel失败");
        }
    }
}
