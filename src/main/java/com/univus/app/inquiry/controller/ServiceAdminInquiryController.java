package com.univus.app.inquiry.controller;

import com.univus.app.inquiry.dto.InquiryDto;
import com.univus.app.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/service-admin/inquiries")
@RequiredArgsConstructor
public class ServiceAdminInquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    public List<InquiryDto.Room> getRooms(Authentication authentication) {
        return inquiryService.getServiceAdminRooms(memberId(authentication));
    }

    @GetMapping("/{roomId}")
    public InquiryDto.Thread getThread(
            Authentication authentication,
            @PathVariable Long roomId
    ) {
        return inquiryService.getServiceAdminThread(memberId(authentication), roomId);
    }

    @PostMapping("/{roomId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public InquiryDto.Message sendMessage(
            Authentication authentication,
            @PathVariable Long roomId,
            @ModelAttribute InquiryDto.SendMessageRequest request,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        return inquiryService.sendServiceAdminMessage(memberId(authentication), roomId, request, files);
    }

    @PatchMapping("/{roomId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeRoom(
            Authentication authentication,
            @PathVariable Long roomId
    ) {
        inquiryService.closeServiceAdminRoom(memberId(authentication), roomId);
    }

    private Long memberId(Authentication authentication) {
        return Long.valueOf(authentication.getPrincipal().toString());
    }
}
