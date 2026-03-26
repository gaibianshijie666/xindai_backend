package com.xindai.xindai.modules.collection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 催收任务实体
 */
@Data
@TableName("collection_task")
public class CollectionTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("contract_id")
    private Long contractId;

    @TableField("user_id")
    private Long userId;

    @TableField("overdue_amount")
    private BigDecimal overdueAmount;

    @TableField("overdue_days")
    private Integer overdueDays;

    @TableField("collector_id")
    private Long collectorId;

    /**
     * 状态: 0=待分配,1=已分配,2=处理中,3=已完成,4=已关闭
     * @see com.xindai.xindai.modules.collection.enums.CollectionTaskStatus
     */
    private Integer status;

    /**
     * 优先级: 1=低,2=中,3=高
     */
    private Integer priority;

    @TableField("deadline")
    private LocalDateTime deadline;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
