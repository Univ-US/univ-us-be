package com.univus.app.inquiry.service;

import com.univus.app.inquiry.dto.InquiryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InquiryService {

    List<InquiryDto.Room> getAdminRooms(Long memberId);

    List<InquiryDto.Room> getServiceAdminRooms(Long memberId);

    InquiryDto.Thread getAdminThread(Long memberId, Long roomId);

    InquiryDto.Thread getServiceAdminThread(Long memberId, Long roomId);

    InquiryDto.Room createRoom(
            Long memberId,
            InquiryDto.CreateRequest request,
            List<MultipartFile> files
    );

    InquiryDto.Message sendAdminMessage(
            Long memberId,
            Long roomId,
            InquiryDto.SendMessageRequest request,
            List<MultipartFile> files
    );

    InquiryDto.Message sendServiceAdminMessage(
            Long memberId,
            Long roomId,
            InquiryDto.SendMessageRequest request,
            List<MultipartFile> files
    );

    void closeAdminRoom(Long memberId, Long roomId);

    void closeServiceAdminRoom(Long memberId, Long roomId);

    ResponseEntity<?> downloadAttachment(Long memberId, Long attachmentId);
}
