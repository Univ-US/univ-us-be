package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsProfChatDto;
import com.univus.app.lms.service.LmsProfChatService;
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
@RequestMapping("/api/lms/professor/chats")
@RequiredArgsConstructor
public class LmsProfChatController {

    private final LmsProfChatService lmsProfChatService;

    @GetMapping
    public ResponseEntity<?> requestGetRooms(Authentication authentication) {
        try {
            Long memberId = Long.valueOf(authentication.getPrincipal().toString());
            return ResponseEntity.ok(lmsProfChatService.getChatRooms(memberId));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> requestGetUnreadCount(Authentication authentication) {
        try {
            Long memberId = Long.valueOf(authentication.getPrincipal().toString());
            return ResponseEntity.ok(Map.of("count", lmsProfChatService.getUnreadCount(memberId)));
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
            return ResponseEntity.ok(lmsProfChatService.getChatThread(memberId, roomId));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    @PostMapping("/{roomId}/messages")
    public ResponseEntity<?> requestSendMessage(
            Authentication authentication,
            @PathVariable Long roomId,
            @RequestBody LmsProfChatDto.SendMessageReqDto request) {
        try {
            Long memberId = Long.valueOf(authentication.getPrincipal().toString());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(lmsProfChatService.sendMessage(memberId, roomId, request));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    private ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", ex.getMessage()));
    }
}
