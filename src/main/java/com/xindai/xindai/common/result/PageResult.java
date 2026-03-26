package com.xindai.xindai.common.result;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 分页返回结构
 */
@Data
public class PageResult<T> implements Serializable {
    private List<T> list;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;

    public static <T> PageResult<T> of(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }
}
