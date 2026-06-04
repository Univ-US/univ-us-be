package com.univus.app.community.controller;

import com.univus.app.community.dto.PostDto;
import com.univus.app.community.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    @PostMapping
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
}