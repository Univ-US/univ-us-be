package com.univus.app.cmypage.dto;

import lombok.Data;

@Data
public class CmypageWishlistDto {
    private Long productId;
    private String productName;
    private Long price;
    private String place;
    private String status;
}
