package com.univus.app.community.mapper;

import com.univus.app.community.dto.PostCommentDto;
import com.univus.app.community.dto.PostDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PostMapper {

    // 게시글 목록 조회 (페이징 + 검색)
    List<PostDto> selectPostList(PostDto postDto);

    // 게시글 전체 개수 (페이징 계산용)
    int selectPostCount(PostDto postDto);

    // 게시글 단건 조회
    PostDto selectPostById(Long postId);

    // 게시글 등록
    int insertPost(PostDto postDto);

    // 게시글 수정
    int updatePost(PostDto postDto);

    // 게시글 삭제 (소프트 삭제 - IS_DELETED = 1)
    int deletePost(Long postId);

    // 조회수 증가
    int updateViewCount(Long postId);

    // ── 좋아요 ──────────────────────────────────────────────────────

    // 좋아요 여부 확인 (0 or 1)
    int selectLikeCount(Long postId, Long memberId);

    // 좋아요 추가
    int insertLike(Long postId, Long memberId);

    // 좋아요 취소
    int deleteLike(Long postId, Long memberId);
    
	 // ── 댓글 ──────────────────────────────────────────────
	
	 // 댓글 목록 조회 (postId 기준)
	 List<PostCommentDto> selectCommentList(Long postId);
	
	 // 댓글 등록
	 int insertComment(PostCommentDto commentDto);
	
	 // 댓글 수정
	 int updateComment(PostCommentDto commentDto);
	
	 // 댓글 삭제 (소프트 삭제)
	 int deleteComment(Long commentId);
}