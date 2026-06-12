package com.univus.app.cmypage.service;

import com.univus.app.cmypage.dto.CmypageCommentDto;
import com.univus.app.cmypage.mapper.CmypageMapper;
import com.univus.app.community.dto.PostDto;
import com.univus.app.community.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CmypageService {

    private final PostService postService;
    private final CmypageMapper cmypageMapper;

    public Map<String, Object> getMyPosts(Long memberId, PostDto postDto) {
        postDto.setMemberId(memberId);
        return postService.getPostList(postDto);
    }

    public List<CmypageCommentDto> getMyComments(Long memberId) {
        return cmypageMapper.selectMyComments(memberId);
    }

    public List<PostDto> getLikedPosts(Long memberId) {
        return cmypageMapper.selectLikedPosts(memberId);
    }
}
