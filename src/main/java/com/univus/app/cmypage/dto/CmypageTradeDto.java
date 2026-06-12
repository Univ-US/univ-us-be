package com.univus.app.cmypage.dto;

import lombok.Data;
import java.util.Date;

@Data
public class CmypageTradeDto {
    private Long tradeId;
    private String productName;
    private Long price;
    private String status;
    private String role;
    private Date createdAt;
}
