package com.univus.app.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostImageDto {
    private Long imageId;
    private Long postId;
    private String imageUrl;
    private Integer imageSort;
    private LocalDateTime createdAt;
}
