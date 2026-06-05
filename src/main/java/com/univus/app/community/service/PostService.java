package com.univus.app.community.service;

import com.univus.app.community.dto.PostDto;
import com.univus.app.community.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.univus.app.community.dto.PostCommentDto;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostMapper postMapper;

    // 게시글 목록 조회 (페이징 + 검색)
    public Map<String, Object> getPostList(PostDto postDto) {

        // 기본값 처리
        if (postDto.getPage() <= 0) postDto.setPage(1);
        if (postDto.getSize() <= 0) postDto.setSize(10);

        // 전체 게시글 수
        int totalCount = postMapper.selectPostCount(postDto);

        // 게시글 목록
        List<PostDto> postList = postMapper.selectPostList(postDto);

        // 전체 페이지 수 계산
        int totalPage = (int) Math.ceil((double) totalCount / postDto.getSize());

        Map<String, Object> result = new HashMap<>();
        result.put("postList", postList);
        result.put("totalCount", totalCount);
        result.put("totalPage", totalPage);
        result.put("currentPage", postDto.getPage());

        return result;
    }

    // 게시글 단건 조회 + 조회수 증가
    public PostDto getPostById(Long postId) {
        postMapper.updateViewCount(postId);
        return postMapper.selectPostById(postId);
    }

    // 게시글 등록
    public int writePost(PostDto postDto) {
        return postMapper.insertPost(postDto);
    }

    // 게시글 수정
    public int modifyPost(PostDto postDto) {
        return postMapper.updatePost(postDto);
    }

    // 게시글 삭제
    public int removePost(Long postId) {
        return postMapper.deletePost(postId);
    }
    
	 // ── 댓글 ──────────────────────────────────────────────
	
	 // 댓글 목록 조회
	 public List<PostCommentDto> getCommentList(Long postId) {
	     return postMapper.selectCommentList(postId);
	 }
	
	 // 댓글 등록
	 public int writeComment(PostCommentDto commentDto) {
	     return postMapper.insertComment(commentDto);
	 }
	
	 // 댓글 수정
	 public int modifyComment(PostCommentDto commentDto) {
	     return postMapper.updateComment(commentDto);
	 }
	
	 // 댓글 삭제
	 public int removeComment(Long commentId) {
	     return postMapper.deleteComment(commentId);
	 }
}