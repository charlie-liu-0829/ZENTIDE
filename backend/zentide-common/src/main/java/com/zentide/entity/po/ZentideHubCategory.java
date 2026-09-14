package com.zentide.entity.po;

import lombok.Data;

@Data
public class ZentideHubCategory {
    private Long categoryId;
    private String userId;
    private String name;
    private Integer sortOrder;
    private Integer hubCount;
}
