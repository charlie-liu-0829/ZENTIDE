package com.zentide.entity.vo;

import lombok.Data;

@Data
public class ZentideSearchResult {
    private String type;
    private String id;
    private String title;
    private String summary;
    private String hubName;
    private String category;
    private Integer memberCount;
    private Integer postCount;
    private Double score;
}
