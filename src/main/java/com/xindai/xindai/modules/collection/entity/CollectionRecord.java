package com.xindai.xindai.modules.collection.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 催收记录实体
 */
@Data
@TableName("collection_record")
public class CollectionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("collector_id")
    private Long collectorId;

    /**
     * 催收方式: phone/sms/visit/legal
     */
    private String method;

    /**
     * 催收内容
     */
    private String content;

    /**
     * 催收结果: promise_pay/refused/unreachable/other
     */
    private String result;

    @TableField("next_follow_up_date")
    private LocalDate nextFollowUpDate;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
