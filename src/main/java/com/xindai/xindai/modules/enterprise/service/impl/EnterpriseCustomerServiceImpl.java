package com.xindai.xindai.modules.enterprise.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.enterprise.dto.*;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseCustomer;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseCustomerMapper;
import com.xindai.xindai.modules.enterprise.service.EnterpriseCustomerService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 企业客户服务实现
 */
@Service
@RequiredArgsConstructor
public class EnterpriseCustomerServiceImpl implements EnterpriseCustomerService {

    private final EnterpriseCustomerMapper customerMapper;

    @Override
    public Page<EnterpriseCustomerVO> list(Long enterpriseId, EnterpriseCustomerQueryDTO queryDTO) {
        LambdaQueryWrapper<EnterpriseCustomer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnterpriseCustomer::getEnterpriseId, enterpriseId);
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w
                    .like(EnterpriseCustomer::getRealName, queryDTO.getKeyword())
                    .or()
                    .like(EnterpriseCustomer::getPhone, queryDTO.getKeyword())
                    .or()
                    .like(EnterpriseCustomer::getIdCard, queryDTO.getKeyword())
            );
        }
        wrapper.orderByDesc(EnterpriseCustomer::getCreatedAt);

        Page<EnterpriseCustomer> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        Page<EnterpriseCustomer> result = customerMapper.selectPage(pageParam, wrapper);

        // 转换为VO
        Page<EnterpriseCustomerVO> voPage = new Page<>();
        voPage.setTotal(result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public EnterpriseCustomerVO create(Long enterpriseId, EnterpriseCustomerDTO dto) {
        // 检查身份证是否已存在
        Long count = customerMapper.selectCount(
                new LambdaQueryWrapper<EnterpriseCustomer>()
                        .eq(EnterpriseCustomer::getEnterpriseId, enterpriseId)
                        .eq(EnterpriseCustomer::getIdCard, dto.getIdCard())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.ENTERPRISE_CUSTOMER_NOT_FOUND, "该身份证号已存在");
        }

        EnterpriseCustomer customer = new EnterpriseCustomer();
        BeanUtils.copyProperties(dto, customer);
        customer.setEnterpriseId(enterpriseId);
        customer.setCustomerNo(generateCustomerNo());
        customer.setStatus(0);
        customer.setCreditScore(600);
        customer.setRiskLevel(1);
        customer.setTotalLoanCount(0);
        customer.setTotalLoanAmount(java.math.BigDecimal.ZERO);
        customerMapper.insert(customer);

        return toVO(customer);
    }

    @Override
    public EnterpriseCustomerVO update(Long enterpriseId, Long customerId, EnterpriseCustomerDTO dto) {
        EnterpriseCustomer customer = getByEnterpriseAndId(enterpriseId, customerId);

        // 检查身份证是否与其他客户重复
        Long count = customerMapper.selectCount(
                new LambdaQueryWrapper<EnterpriseCustomer>()
                        .eq(EnterpriseCustomer::getEnterpriseId, enterpriseId)
                        .eq(EnterpriseCustomer::getIdCard, dto.getIdCard())
                        .ne(EnterpriseCustomer::getId, customerId)
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.ENTERPRISE_CUSTOMER_NOT_FOUND, "该身份证号已存在");
        }

        customer.setRealName(dto.getRealName());
        customer.setIdCard(dto.getIdCard());
        customer.setPhone(dto.getPhone());
        customerMapper.updateById(customer);

        return toVO(customer);
    }

    @Override
    public void delete(Long enterpriseId, Long customerId) {
        EnterpriseCustomer customer = getByEnterpriseAndId(enterpriseId, customerId);
        customerMapper.deleteById(customerId);
    }

    @Override
    public EnterpriseCustomer getByEnterpriseAndId(Long enterpriseId, Long customerId) {
        EnterpriseCustomer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<EnterpriseCustomer>()
                        .eq(EnterpriseCustomer::getEnterpriseId, enterpriseId)
                        .eq(EnterpriseCustomer::getId, customerId)
        );
        if (customer == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_CUSTOMER_NOT_FOUND);
        }
        return customer;
    }

    @Override
    public void batchImport(Long enterpriseId, List<EnterpriseCustomerDTO> customers) {
        for (EnterpriseCustomerDTO dto : customers) {
            try {
                create(enterpriseId, dto);
            } catch (BusinessException e) {
                // 跳过已存在的客户，继续导入
                if (!e.getMessage().contains("已存在")) {
                    throw e;
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResultVO batchDelete(Long enterpriseId, List<Long> ids) {
        List<BatchOperationResultVO.FailDetail> failDetails = new ArrayList<>();
        int successCount = 0;

        for (Long id : ids) {
            try {
                // 验证客户是否属于该企业
                EnterpriseCustomer customer = customerMapper.selectOne(
                        new LambdaQueryWrapper<EnterpriseCustomer>()
                                .eq(EnterpriseCustomer::getEnterpriseId, enterpriseId)
                                .eq(EnterpriseCustomer::getId, id)
                );
                if (customer == null) {
                    failDetails.add(new BatchOperationResultVO.FailDetail(id, "客户不存在或无权操作"));
                    continue;
                }

                customerMapper.deleteById(id);
                successCount++;
            } catch (Exception e) {
                failDetails.add(new BatchOperationResultVO.FailDetail(id, "删除失败: " + e.getMessage()));
            }
        }

        return BatchOperationResultVO.of(ids.size(), successCount, failDetails);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResultVO batchUpdateStatus(Long enterpriseId, BatchUpdateStatusDTO dto) {
        List<BatchOperationResultVO.FailDetail> failDetails = new ArrayList<>();
        int successCount = 0;

        for (Long id : dto.getIds()) {
            try {
                EnterpriseCustomer customer = customerMapper.selectOne(
                        new LambdaQueryWrapper<EnterpriseCustomer>()
                                .eq(EnterpriseCustomer::getEnterpriseId, enterpriseId)
                                .eq(EnterpriseCustomer::getId, id)
                );
                if (customer == null) {
                    failDetails.add(new BatchOperationResultVO.FailDetail(id, "客户不存在或无权操作"));
                    continue;
                }

                customer.setStatus(dto.getStatus());
                customer.setUpdatedAt(LocalDateTime.now());
                customerMapper.updateById(customer);
                successCount++;
            } catch (Exception e) {
                failDetails.add(new BatchOperationResultVO.FailDetail(id, "更新失败: " + e.getMessage()));
            }
        }

        return BatchOperationResultVO.of(dto.getIds().size(), successCount, failDetails);
    }

    @Override
    public void export(Long enterpriseId, List<Long> ids, HttpServletResponse response) {
        // 查询要导出的客户
        LambdaQueryWrapper<EnterpriseCustomer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnterpriseCustomer::getEnterpriseId, enterpriseId);
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(EnterpriseCustomer::getId, ids);
        }
        wrapper.orderByDesc(EnterpriseCustomer::getCreatedAt);

        List<EnterpriseCustomer> customers = customerMapper.selectList(wrapper);

        // 转换为Excel VO
        List<EnterpriseCustomerExcelVO> excelVOList = customers.stream()
                .map(customer -> {
                    EnterpriseCustomerVO vo = toVO(customer);
                    return EnterpriseCustomerExcelVO.fromVO(vo);
                })
                .collect(Collectors.toList());

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("客户列表", StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 写入Excel
        try {
            EasyExcel.write(response.getOutputStream(), EnterpriseCustomerExcelVO.class)
                    .sheet("客户列表")
                    .doWrite(excelVOList);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出Excel失败");
        }
    }

    private String generateCustomerNo() {
        return "EC" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }

    private EnterpriseCustomerVO toVO(EnterpriseCustomer entity) {
        EnterpriseCustomerVO vo = new EnterpriseCustomerVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
