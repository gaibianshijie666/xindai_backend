package com.xindai.xindai.modules.enterprise.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作结果VO
 */
@Data
public class BatchOperationResultVO {
    /**
     * 总数
     */
    private Integer totalCount;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failCount;

    /**
     * 失败详情
     */
    private List<FailDetail> failDetails;

    public BatchOperationResultVO() {
        this.failDetails = new ArrayList<>();
    }

    public static BatchOperationResultVO of(int total, int success, List<FailDetail> fails) {
        BatchOperationResultVO result = new BatchOperationResultVO();
        result.setTotalCount(total);
        result.setSuccessCount(success);
        result.setFailCount(total - success);
        result.setFailDetails(fails);
        return result;
    }

    @Data
    public static class FailDetail {
        /**
         * 记录ID
         */
        private Long id;

        /**
         * 失败原因
         */
        private String reason;

        public FailDetail() {
        }

        public FailDetail(Long id, String reason) {
            this.id = id;
            this.reason = reason;
        }
    }
}
