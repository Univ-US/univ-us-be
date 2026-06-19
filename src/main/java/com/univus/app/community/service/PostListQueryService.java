package com.univus.app.community.service;

import com.univus.app.community.dto.PostDto;
import com.univus.app.community.dto.PostListResponseDto;

public interface PostListQueryService {

    PostListResponseDto getPostList(PostDto postDto, Long memberId);

    PostListResponseDto getPostList(PostDto postDto);
}
