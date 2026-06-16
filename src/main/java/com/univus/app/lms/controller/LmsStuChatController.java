package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsStuChatDto;
import com.univus.app.lms.service.LmsStuChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/lms/student/chats")
@RequiredArgsConstructor
public class LmsStuChatController {

    private final LmsStuChatService lmsStuChatService;

    @GetMapping
    public ResponseEntity<?> requestGetRooms(Authentication authentication) {
        try {
            Long memberId = Long.valueOf(authentication.getPrincipal().toString());
            return ResponseEntity.ok(lmsStuChatService.getChatRooms(memberId));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> requestGetUnreadCount(Authentication authentication) {
        try {
            Long memberId = Long.valueOf(authentication.getPrincipal().toString());
            return ResponseEntity.ok(Map.of("count", lmsStuChatService.getUnreadCount(memberId)));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> requestGetThread(
            Authentication authentication,
            @PathVariable Long roomId) {
        try {
            Long memberId = Long.valueOf(authentication.getPrincipal().toString());
            return ResponseEntity.ok(lmsStuChatService.getChatThread(memberId, roomId));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @PostMapping("/{roomId}/messages")
    public ResponseEntity<?> requestSendMessage(
            Authentication authentication,
            @PathVariable Long roomId,
            @RequestBody LmsStuChatDto.SendMessageReqDto request) {
        try {
            Long memberId = Long.valueOf(authentication.getPrincipal().toString());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(lmsStuChatService.sendMessage(memberId, roomId, request));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    private ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", ex.getMessage()));
    }
}
