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

    // 좋아요 토글 (없으면 추가, 있으면 취소)
    public Map<String, Object> toggleLike(Long postId, Long memberId) {
        int exists = postMapper.selectLikeCount(postId, memberId);
        Map<String, Object> result = new HashMap<>();
        if (exists > 0) {
            postMapper.deleteLike(postId, memberId);
            result.put("liked", false);
        } else {
            postMapper.insertLike(postId, memberId);
            result.put("liked", true);
        }
        return result;
    }

    // 좋아요 여부 확인
    public boolean isLiked(Long postId, Long memberId) {
        return postMapper.selectLikeCount(postId, memberId) > 0;
    }

    // ── 신고 ──────────────────────────────────────

    // 신고 (중복 신고 차단)
    public Map<String, Object> reportPost(PostDto postDto) {
        Map<String, Object> result = new HashMap<>();
        int exists = postMapper.selectReportCount(postDto.getPostId(), postDto.getMemberId());
        if (exists > 0) {
            result.put("success", false);
            result.put("message", "이미 신고한 게시글입니다.");
            return result;
        }
        postMapper.insertReport(postDto);
        result.put("success", true);
        result.put("message", "신고가 접수되었습니다.");
        return result;
    }

    // 신고 여부 확인
    public boolean isReported(Long postId, Long memberId) {
        return postMapper.selectReportCount(postId, memberId) > 0;
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