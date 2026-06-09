package com.univus.app.community.controller;

import com.univus.app.community.dto.PostDto;
import com.univus.app.community.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import com.univus.app.community.dto.PostCommentDto;
import com.univus.app.community.dto.PostImageDto;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // GET /api/posts?page=1&size=10&keyword=검색어&boardId=1
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPostList(PostDto postDto) {
        Map<String, Object> result = postService.getPostList(postDto);
        return ResponseEntity.ok(result);
    }

    // GET /api/posts/{postId}
    @GetMapping("/{postId}")
    public ResponseEntity<PostDto> getPostById(@PathVariable("postId") Long postId) {
        PostDto post = postService.getPostById(postId);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(post);
    }

    // POST /api/posts
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> writePost(@RequestBody PostDto postDto) {
        // 임시 멤버 ID (로그인 구현 전)
        postDto.setMemberId(1L);

        int result = postService.writePost(postDto);
        if (result > 0) {
            return ResponseEntity.ok(Map.of("message", "게시글이 등록되었습니다."));
        }
        return ResponseEntity.internalServerError()
                .body(Map.of("message", "게시글 등록에 실패했습니다."));
    }

    // POST /api/posts multipart
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> writePostWithImages(
            @RequestParam("boardId") Long boardId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        PostDto postDto = new PostDto();
        postDto.setMemberId(1L);
        postDto.setBoardId(boardId);
        postDto.setTitle(title);
        postDto.setContent(content);
        postDto.setCategory(category);

        int result = postService.writePost(postDto);
        if (result <= 0) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Post creation failed."));
        }

        List<PostImageDto> uploadedImages = postService.uploadPostImages(postDto.getPostId(), images);
        return ResponseEntity.ok(Map.of(
                "message", "Post created successfully.",
                "postId", postDto.getPostId(),
                "images", uploadedImages
        ));
    }

    // PUT /api/posts/{postId}
    @PutMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> modifyPost(
            @PathVariable("postId") Long postId,
            @RequestBody PostDto postDto) {
        postDto.setPostId(postId);

        int result = postService.modifyPost(postDto);
        if (result > 0) {
            return ResponseEntity.ok(Map.of("message", "게시글이 수정되었습니다."));
        }
        return ResponseEntity.internalServerError()
                .body(Map.of("message", "게시글 수정에 실패했습니다."));
    }

    // DELETE /api/posts/{postId}
    @DeleteMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> removePost(@PathVariable("postId") Long postId) {
        int result = postService.removePost(postId);
        if (result > 0) {
            return ResponseEntity.ok(Map.of("message", "게시글이 삭제되었습니다."));
        }
        return ResponseEntity.internalServerError()
                .body(Map.of("message", "게시글 삭제에 실패했습니다."));
    }

    // ── 좋아요 ──────────────────────────────────────────────

    // POST /api/posts/{postId}/like — 좋아요 토글 (로그인 구현 전 임시 memberId=1)
    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable("postId") Long postId) {
        Long memberId = 1L;
        Map<String, Object> result = postService.toggleLike(postId, memberId);
        return ResponseEntity.ok(result);
    }

    // GET /api/posts/{postId}/like — 좋아요 여부 확인
    @GetMapping("/{postId}/like")
    public ResponseEntity<Map<String, Object>> getLikeStatus(@PathVariable("postId") Long postId) {
        Long memberId = 1L;
        boolean liked = postService.isLiked(postId, memberId);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    // POST /api/posts/{postId}/report — 신고
    @PostMapping("/{postId}/report")
    public ResponseEntity<Map<String, Object>> reportPost(
            @PathVariable("postId") Long postId,
            @RequestBody PostDto postDto) {
        postDto.setPostId(postId);
        postDto.setMemberId(1L); // TODO: JWT 구현 후 토큰에서 추출
        Map<String, Object> result = postService.reportPost(postDto);
        return ResponseEntity.ok(result);
    }

    // GET /api/posts/{postId}/report — 신고 여부 확인
    @GetMapping("/{postId}/report")
    public ResponseEntity<Map<String, Object>> getReportStatus(@PathVariable("postId") Long postId) {
        Long memberId = 1L; // TODO: JWT 구현 후 토큰에서 추출
        boolean reported = postService.isReported(postId, memberId);
        return ResponseEntity.ok(Map.of("reported", reported));
    }
    
	 // ── 댓글 ──────────────────────────────────────────────
	
	 // GET /api/posts/{postId}/comments
	 @GetMapping("/{postId}/comments")
	 public ResponseEntity<List<PostCommentDto>> getCommentList(@PathVariable("postId") Long postId) {
	     List<PostCommentDto> commentList = postService.getCommentList(postId);
	     return ResponseEntity.ok(commentList);
	 }
	
	 // POST /api/posts/{postId}/comments
	 @PostMapping("/{postId}/comments")
	 public ResponseEntity<Map<String, Object>> writeComment(
	         @PathVariable("postId") Long postId,
	         @RequestBody PostCommentDto commentDto) {
	     // 임시 멤버 ID (로그인 구현 전)
	     commentDto.setMemberId(1L);
	     commentDto.setPostId(postId);
	
	     int result = postService.writeComment(commentDto);
	     if (result > 0) {
	         return ResponseEntity.ok(Map.of("message", "댓글이 등록되었습니다."));
	     }
	     return ResponseEntity.internalServerError()
	             .body(Map.of("message", "댓글 등록에 실패했습니다."));
	 }
	
	 // PUT /api/posts/{postId}/comments/{commentId}
	 @PutMapping("/{postId}/comments/{commentId}")
	 public ResponseEntity<Map<String, Object>> modifyComment(
	         @PathVariable("postId") Long postId,
	         @PathVariable("commentId") Long commentId,
	         @RequestBody PostCommentDto commentDto) {
	     commentDto.setCommentId(commentId);
	     commentDto.setPostId(postId);
	
	     int result = postService.modifyComment(commentDto);
	     if (result > 0) {
	         return ResponseEntity.ok(Map.of("message", "댓글이 수정되었습니다."));
	     }
	     return ResponseEntity.internalServerError()
	             .body(Map.of("message", "댓글 수정에 실패했습니다."));
	 }
	
	 // DELETE /api/posts/{postId}/comments/{commentId}
	 @DeleteMapping("/{postId}/comments/{commentId}")
	 public ResponseEntity<Map<String, Object>> removeComment(
	         @PathVariable("postId") Long postId,
	         @PathVariable("commentId") Long commentId) {
	     int result = postService.removeComment(commentId);
	     if (result > 0) {
	         return ResponseEntity.ok(Map.of("message", "댓글이 삭제되었습니다."));
	     }
	     return ResponseEntity.internalServerError()
	             .body(Map.of("message", "댓글 삭제에 실패했습니다."));
	 }
    // POST /api/posts/{postId}/images
    @PostMapping("/{postId}/images")
    public ResponseEntity<Map<String, Object>> uploadPostImages(
            @PathVariable("postId") Long postId,
            @RequestParam("images") List<MultipartFile> images) {
        List<PostImageDto> uploadedImages = postService.uploadPostImages(postId, images);
        return ResponseEntity.ok(Map.of(
                "message", "Images uploaded successfully.",
                "images", uploadedImages
        ));
    }

    // GET /api/posts/{postId}/images
    @GetMapping("/{postId}/images")
    public ResponseEntity<List<PostImageDto>> getPostImages(@PathVariable("postId") Long postId) {
        return ResponseEntity.ok(postService.getPostImageList(postId));
    }
}
