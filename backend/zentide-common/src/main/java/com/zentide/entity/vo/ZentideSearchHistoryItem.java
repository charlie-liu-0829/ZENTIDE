package com.zentide.entity.vo;

import lombok.Data;

@Data
public class ZentideSearchHistoryItem {
    private String query;
    private String type;
    private String searchedAt;
}
