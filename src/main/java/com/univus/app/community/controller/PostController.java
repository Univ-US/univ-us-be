package com.univus.app.community.controller;

import com.univus.app.community.dto.PostDto;
import com.univus.app.community.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
    public ResponseEntity<Map<String, Object>> getPostList(
            PostDto postDto,
            @AuthenticationPrincipal Long memberId) {
        Map<String, Object> result = postService.getPostList(postDto, requireMemberId(memberId));
        return ResponseEntity.ok(result);
    }

    // GET /api/posts/{postId}
    @GetMapping("/{postId}")
    public ResponseEntity<PostDto> getPostById(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal Long memberId) {
        PostDto post = postService.getPostById(postId, requireMemberId(memberId));
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(post);
    }

    @PostMapping("/{postId}/view")
    public ResponseEntity<PostDto> increaseViewCount(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(postService.increaseViewCount(postId, requireMemberId(memberId)));
    }

    // POST /api/posts
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> writePost(
            @AuthenticationPrincipal Long memberId,
            Authentication authentication,
            @RequestBody PostDto postDto) {
        Long currentMemberId = requireMemberId(memberId);
        if (isNoticeBoard(postDto.getBoardId()) && !isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "공지사항 작성 권한이 없습니다.");
        }
        postDto.setMemberId(currentMemberId);

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
            @RequestParam(value = "univId", required = false) Long univId,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal Long memberId,
            Authentication authentication) {
        Long currentMemberId = requireMemberId(memberId);
        if (isNoticeBoard(boardId) && !isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "공지사항 작성 권한이 없습니다.");
        }
        PostDto postDto = new PostDto();
        postDto.setMemberId(currentMemberId);
        postDto.setBoardId(boardId);
        postDto.setTitle(title);
        postDto.setContent(content);
        postDto.setCategory(category);
        postDto.setUnivId(univId);

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
            @AuthenticationPrincipal Long memberId,
            Authentication authentication,
            @RequestBody PostDto postDto) {
        assertCanManagePost(postId, requireMemberId(memberId), authentication);
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
    public ResponseEntity<Map<String, Object>> removePost(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal Long memberId,
            Authentication authentication) {
        assertCanManagePost(postId, requireMemberId(memberId), authentication);
        int result = postService.removePost(postId);
        if (result > 0) {
            return ResponseEntity.ok(Map.of("message", "게시글이 삭제되었습니다."));
        }
        return ResponseEntity.internalServerError()
                .body(Map.of("message", "게시글 삭제에 실패했습니다."));
    }

    // ── 좋아요 ──────────────────────────────────────────────

    // POST /api/posts/{postId}/like — 좋아요 토글
    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal Long memberId) {
        Map<String, Object> result = postService.toggleLike(postId, requireMemberId(memberId));
        return ResponseEntity.ok(result);
    }

    // GET /api/posts/{postId}/like — 좋아요 여부 확인
    @GetMapping("/{postId}/like")
    public ResponseEntity<Map<String, Object>> getLikeStatus(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal Long memberId) {
        boolean liked = postService.isLiked(postId, requireMemberId(memberId));
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    // POST /api/posts/{postId}/report — 신고
    @PostMapping("/{postId}/report")
    public ResponseEntity<Map<String, Object>> reportPost(
            @PathVariable("postId") Long postId,
            @RequestBody PostDto postDto,
            @AuthenticationPrincipal Long memberId) {
        postDto.setPostId(postId);
        postDto.setMemberId(requireMemberId(memberId));
        Map<String, Object> result = postService.reportPost(postDto);
        return ResponseEntity.ok(result);
    }

    // GET /api/posts/{postId}/report — 신고 여부 확인
    @GetMapping("/{postId}/report")
    public ResponseEntity<Map<String, Object>> getReportStatus(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal Long memberId) {
        boolean reported = postService.isReported(postId, requireMemberId(memberId));
        return ResponseEntity.ok(Map.of("reported", reported));
    }
    
	 // ── 댓글 ──────────────────────────────────────────────
	
	 // GET /api/posts/{postId}/comments
	 @GetMapping("/{postId}/comments")
	 public ResponseEntity<List<PostCommentDto>> getCommentList(
             @PathVariable("postId") Long postId,
             @AuthenticationPrincipal Long memberId) {
	     List<PostCommentDto> commentList = postService.getCommentList(postId, requireMemberId(memberId));
	     return ResponseEntity.ok(commentList);
	 }
	
	 // POST /api/posts/{postId}/comments
	 @PostMapping("/{postId}/comments")
	 public ResponseEntity<Map<String, Object>> writeComment(
	         @PathVariable("postId") Long postId,
	         @RequestBody PostCommentDto commentDto,
	         @AuthenticationPrincipal Long memberId) {
	     commentDto.setMemberId(requireMemberId(memberId));
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
             @AuthenticationPrincipal Long memberId,
             Authentication authentication,
	         @RequestBody PostCommentDto commentDto) {
         assertCanManageComment(postId, commentId, requireMemberId(memberId), authentication);
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
	         @PathVariable("commentId") Long commentId,
             @AuthenticationPrincipal Long memberId,
             Authentication authentication) {
         assertCanManageComment(postId, commentId, requireMemberId(memberId), authentication);
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
            @AuthenticationPrincipal Long memberId,
            @RequestParam("images") List<MultipartFile> images) {
        postService.assertPostAccessible(postId, requireMemberId(memberId));
        List<PostImageDto> uploadedImages = postService.uploadPostImages(postId, images);
        return ResponseEntity.ok(Map.of(
                "message", "Images uploaded successfully.",
                "images", uploadedImages
        ));
    }

    // GET /api/posts/{postId}/images
    @GetMapping("/{postId}/images")
    public ResponseEntity<List<PostImageDto>> getPostImages(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(postService.getPostImageList(postId, requireMemberId(memberId)));
    }

    private Long requireMemberId(Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return memberId;
    }

    private void assertCanManagePost(Long postId, Long memberId, Authentication authentication) {
        PostDto post = postService.findPostById(postId);
        if (post == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        postService.assertPostAccessible(postId, memberId);
        if (!post.getMemberId().equals(memberId) && !isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "게시글 수정/삭제 권한이 없습니다.");
        }
    }

    private void assertCanManageComment(Long postId, Long commentId, Long memberId, Authentication authentication) {
        PostCommentDto comment = postService.findCommentById(commentId);
        if (comment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }
        if (!comment.getPostId().equals(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }
        postService.assertPostAccessible(comment.getPostId(), memberId);
        if (!comment.getMemberId().equals(memberId) && !isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "댓글 수정/삭제 권한이 없습니다.");
        }
    }

    private boolean isNoticeBoard(Long boardId) {
        return Long.valueOf(3L).equals(boardId);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_SUA".equals(authority.getAuthority())
                                || "ROLE_ADM".equals(authority.getAuthority()));
    }
}
