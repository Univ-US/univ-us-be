package com.univus.app.community.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostListResponseDto {
    private List<PostDto> postList;
    private Integer totalCount;
    private Integer todayCount;
    private Integer totalPage;
    private Integer currentPage;
}
