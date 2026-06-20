package com.univus.app.community.service;

import com.univus.app.community.dto.PostCommentDto;
import com.univus.app.community.dto.PostDto;
import com.univus.app.community.dto.PostImageDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface PostService extends PostListQueryService {

    PostDto getPostById(Long postId, Long memberId);

    PostDto increaseViewCount(Long postId, Long memberId);

    PostDto findPostById(Long postId);

    int writePost(PostDto postDto);

    int modifyPost(PostDto postDto);

    int removePost(Long postId);

    Map<String, Object> toggleLike(Long postId, Long memberId);

    boolean isLiked(Long postId, Long memberId);

    Map<String, Object> reportPost(PostDto postDto);

    boolean isReported(Long postId, Long memberId);

    List<PostCommentDto> getCommentList(Long postId, Long memberId);

    PostCommentDto findCommentById(Long commentId);

    int writeComment(PostCommentDto commentDto);

    int modifyComment(PostCommentDto commentDto);

    int removeComment(Long commentId);

    List<PostImageDto> uploadPostImages(Long postId, List<MultipartFile> images);

    List<PostImageDto> getPostImageList(Long postId, Long memberId);

    void assertPostAccessible(Long postId, Long memberId);
}
