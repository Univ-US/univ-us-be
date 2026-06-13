package com.univus.app.cmypage.dto;

import lombok.Data;

@Data
public class CmypageSummaryDto {
    private int postCount;
    private int commentCount;
    private int likedPostCount;
    private int tradeCount;
    private int wishlistCount;
}
