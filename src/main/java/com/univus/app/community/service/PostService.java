package com.univus.app.community.service;

import com.univus.app.common.StorageService;
import com.univus.app.community.dto.PostDto;
import com.univus.app.community.dto.PostImageDto;
import com.univus.app.community.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.univus.app.community.dto.PostCommentDto;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostMapper postMapper;
    private final StorageService storageService;

    @Value("${file.upload-root:${user.home}/univus/uploads}")
    private String uploadRoot;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final long MAX_IMAGE_SIZE = 30L * 1024 * 1024;
    private static final String POST_IMAGE_SUBDIR = "community" + File.separator + "post";
    private static final String POST_IMAGE_URL_PREFIX = "/uploads/community/post/";

    // 게시글 목록 조회 (페이징 + 검색)
    public Map<String, Object> getPostList(PostDto postDto) {

        // 기본값 처리
        if (postDto.getPage() <= 0) postDto.setPage(1);
        if (postDto.getSize() <= 0) postDto.setSize(10);

        // 전체 게시글 수
        int totalCount = postMapper.selectPostCount(postDto);
        int todayCount = postMapper.selectTodayPostCount(postDto);

        // 게시글 목록
        List<PostDto> postList = postMapper.selectPostList(postDto);

        // 전체 페이지 수 계산
        int totalPage = (int) Math.ceil((double) totalCount / postDto.getSize());

        Map<String, Object> result = new HashMap<>();
        result.put("postList", postList);
        result.put("totalCount", totalCount);
        result.put("todayCount", todayCount);
        result.put("totalPage", totalPage);
        result.put("currentPage", postDto.getPage());

        return result;
    }

    // 게시글 단건 조회 + 조회수 증가
    public PostDto getPostById(Long postId) {
        postMapper.updateViewCount(postId);
        PostDto post = postMapper.selectPostById(postId);
        if (post != null) {
            post.setImages(postMapper.selectPostImageList(postId));
        }
        return post;
    }

    public PostDto findPostById(Long postId) {
        return postMapper.selectPostById(postId);
    }

    // 게시글 등록
    @Transactional
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
    @Transactional
    public Map<String, Object> reportPost(PostDto postDto) {
        Map<String, Object> result = new HashMap<>();
        int exists = postMapper.selectReportCount(postDto.getPostId(), postDto.getMemberId());
        if (exists > 0) {
            result.put("success", false);
            result.put("message", "이미 신고한 게시글입니다.");
            return result;
        }
        postMapper.insertReport(postDto);
        postMapper.updatePostReportStatus(postDto.getPostId());
        int reportCount = postMapper.selectTotalReportCount(postDto.getPostId());
        result.put("success", true);
        result.put("reportCount", reportCount);
        result.put("blind", reportCount >= 5);
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

     public PostCommentDto findCommentById(Long commentId) {
         return postMapper.selectCommentById(commentId);
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

    @Transactional
    public List<PostImageDto> uploadPostImages(Long postId, List<MultipartFile> images) {
        PostDto post = postMapper.selectPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("Post not found.");
        }
        if (images == null || images.isEmpty()) {
            return postMapper.selectPostImageList(postId);
        }

        int nextSort = postMapper.selectPostImageList(postId).size() + 1;
        String directoryPath = uploadRoot + File.separator + POST_IMAGE_SUBDIR + File.separator + postId;
        String urlPrefix = POST_IMAGE_URL_PREFIX + postId + "/";

        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }
            validatePostImage(image);
            String savedFilename = storageService.uploadFileToServer(image, directoryPath);
            if (savedFilename == null) {
                continue;
            }

            PostImageDto imageDto = PostImageDto.builder()
                    .postId(postId)
                    .imageUrl(urlPrefix + savedFilename)
                    .imageSort(nextSort++)
                    .build();
            postMapper.insertPostImage(imageDto);
        }

        return postMapper.selectPostImageList(postId);
    }

    public List<PostImageDto> getPostImageList(Long postId) {
        return postMapper.selectPostImageList(postId);
    }

    private void validatePostImage(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPG, PNG, and WEBP images can be uploaded.");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image size must be 30MB or less.");
        }
    }
}
