package com.univus.app.inquiry.service;

import com.univus.app.common.StorageService;
import com.univus.app.inquiry.dto.InquiryDto;
import com.univus.app.inquiry.mapper.InquiryMapper;
import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private static final String ROLE_ADMIN = "ADM";
    private static final String ROLE_SERVICE_ADMIN = "SUA";
    private static final String DEFAULT_CATEGORY = "ETC";
    private static final String INQUIRY_TOPIC_PREFIX = "/sub/inquiries/";
    private static final String INQUIRY_ROOMS_TOPIC = "/sub/inquiries/rooms";
    private static final int MAX_TITLE_LENGTH = 50;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final int MAX_FILES_PER_MESSAGE = 3;
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final String ATTACHMENT_SUBDIR = "inquiries";
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "PAYMENT",
            "SUBSCRIPTION",
            "ACCOUNT",
            "ERROR",
            "ETC"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "txt",
            "pdf", "doc", "docx", "hwp", "hwpx", "xls", "xlsx"
    );

    private final InquiryMapper inquiryMapper;
    private final MemberMapper memberMapper;
    private final StorageService storageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${file.upload-root:${user.home}/univus/uploads}")
    private String uploadRoot;

    @Transactional(readOnly = true)
    public List<InquiryDto.Room> getAdminRooms(Long memberId) {
        MemberDto member = requireRole(memberId, ROLE_ADMIN);
        return inquiryMapper.selectAdminRooms(member.getUnivId(), memberId);
    }

    @Transactional(readOnly = true)
    public List<InquiryDto.Room> getServiceAdminRooms(Long memberId) {
        requireRole(memberId, ROLE_SERVICE_ADMIN);
        return inquiryMapper.selectServiceAdminRooms(memberId);
    }

    @Transactional
    public InquiryDto.Thread getAdminThread(Long memberId, Long roomId) {
        MemberDto member = requireRole(memberId, ROLE_ADMIN);
        InquiryDto.Room room = requireAdminRoom(roomId, member.getUnivId());
        inquiryMapper.markIncomingMessagesRead(room.getRoomId(), memberId);
        return toThread(room);
    }

    @Transactional
    public InquiryDto.Thread getServiceAdminThread(Long memberId, Long roomId) {
        requireRole(memberId, ROLE_SERVICE_ADMIN);
        InquiryDto.Room room = requireRoom(roomId);
        inquiryMapper.markIncomingMessagesRead(room.getRoomId(), memberId);
        return toThread(room);
    }

    @Transactional
    public InquiryDto.Room createRoom(
            Long memberId,
            InquiryDto.CreateRequest request,
            List<MultipartFile> files
    ) {
        MemberDto member = requireRole(memberId, ROLE_ADMIN);
        validateTitle(request == null ? null : request.getTitle());
        validateMessage(request == null ? null : request.getMessageText(), files, true);

        InquiryDto.Room room = new InquiryDto.Room();
        room.setUnivId(member.getUnivId());
        room.setTitle(request.getTitle().trim());
        room.setCategory(normalizeCategory(request.getCategory()));
        room.setStatus(InquiryDto.STATUS_WAITING);
        inquiryMapper.insertRoom(room);
        inquiryMapper.insertParticipant(room.getRoomId(), memberId);

        saveMessage(room.getRoomId(), member, request.getMessageText(), files);
        InquiryDto.Room saved = inquiryMapper.selectRoom(room.getRoomId(), memberId);
        runAfterCommit(() -> messagingTemplate.convertAndSend(INQUIRY_ROOMS_TOPIC, saved));
        return saved;
    }

    @Transactional
    public InquiryDto.Message sendAdminMessage(
            Long memberId,
            Long roomId,
            InquiryDto.SendMessageRequest request,
            List<MultipartFile> files
    ) {
        MemberDto member = requireRole(memberId, ROLE_ADMIN);
        InquiryDto.Room room = requireAdminRoom(roomId, member.getUnivId());
        return sendMessage(room, member, request, files, false);
    }

    @Transactional
    public InquiryDto.Message sendServiceAdminMessage(
            Long memberId,
            Long roomId,
            InquiryDto.SendMessageRequest request,
            List<MultipartFile> files
    ) {
        MemberDto member = requireRole(memberId, ROLE_SERVICE_ADMIN);
        InquiryDto.Room room = requireRoom(roomId);
        InquiryDto.Message message = sendMessage(room, member, request, files, true);
        if (InquiryDto.STATUS_WAITING.equals(room.getStatus())) {
            inquiryMapper.updateRoomStatus(roomId, InquiryDto.STATUS_IN_PROGRESS);
        }
        return message;
    }

    @Transactional
    public void closeAdminRoom(Long memberId, Long roomId) {
        MemberDto member = requireRole(memberId, ROLE_ADMIN);
        closeRoom(requireAdminRoom(roomId, member.getUnivId()));
    }

    @Transactional
    public void closeServiceAdminRoom(Long memberId, Long roomId) {
        requireRole(memberId, ROLE_SERVICE_ADMIN);
        closeRoom(requireRoom(roomId));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> downloadAttachment(Long memberId, Long attachmentId) {
        MemberDto member = requireMember(memberId);
        InquiryDto.Attachment attachment = inquiryMapper.selectAttachment(attachmentId);
        if (attachment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found.");
        }
        InquiryDto.Room room = requireRoomForDownload(member, attachment.getMessageId());
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inquiry room not found.");
        }

        String directoryPath = uploadRoot + File.separator + attachment.getFilePath();
        return storageService.downloadFile(
                directoryPath,
                attachment.getSaveName(),
                attachment.getOriginalName()
        );
    }

    private InquiryDto.Message sendMessage(
            InquiryDto.Room room,
            MemberDto member,
            InquiryDto.SendMessageRequest request,
            List<MultipartFile> files,
            boolean serviceAdminMessage
    ) {
        if (InquiryDto.STATUS_CLOSED.equals(room.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Closed inquiry cannot accept messages.");
        }
        validateMessage(request == null ? null : request.getMessageText(), files, false);
        inquiryMapper.insertParticipant(room.getRoomId(), member.getMemberId());
        InquiryDto.Message message = saveMessage(
                room.getRoomId(),
                member,
                request == null ? null : request.getMessageText(),
                files
        );
        if (!serviceAdminMessage && InquiryDto.STATUS_IN_PROGRESS.equals(room.getStatus())) {
            inquiryMapper.updateRoomStatus(room.getRoomId(), InquiryDto.STATUS_WAITING);
        }
        runAfterCommit(() -> messagingTemplate.convertAndSend(
                INQUIRY_TOPIC_PREFIX + room.getRoomId(),
                message
        ));
        runAfterCommit(() -> messagingTemplate.convertAndSend(
                INQUIRY_ROOMS_TOPIC,
                inquiryMapper.selectRoom(room.getRoomId(), member.getMemberId())
        ));
        return message;
    }

    private InquiryDto.Message saveMessage(
            Long roomId,
            MemberDto member,
            String text,
            List<MultipartFile> files
    ) {
        InquiryDto.Message message = new InquiryDto.Message();
        message.setRoomId(roomId);
        message.setMemberId(member.getMemberId());
        message.setSenderName(member.getMemberName());
        message.setSenderRole(member.getRole());
        String content = text == null ? "" : text.trim();
        message.setContent(content.isEmpty() ? " " : content);
        message.setRead(false);
        inquiryMapper.insertMessage(message);

        for (MultipartFile file : safeFiles(files)) {
            validateFile(file);
            String relativePath = ATTACHMENT_SUBDIR + File.separator + roomId + File.separator + message.getMessageId();
            String directoryPath = uploadRoot + File.separator + relativePath;
            String saveName = storageService.uploadFileToServer(file, directoryPath);
            if (saveName == null) {
                continue;
            }

            InquiryDto.Attachment attachment = new InquiryDto.Attachment();
            attachment.setMessageId(message.getMessageId());
            attachment.setOriginalName(file.getOriginalFilename());
            attachment.setSaveName(saveName);
            attachment.setFilePath(relativePath);
            attachment.setFileSize(file.getSize());
            attachment.setFileType(getExtension(file.getOriginalFilename()).toLowerCase(Locale.ROOT));
            inquiryMapper.insertAttachment(attachment);
        }

        InquiryDto.Message saved = inquiryMapper.selectMessage(message.getMessageId());
        fillAttachments(saved);
        return saved;
    }

    private InquiryDto.Thread toThread(InquiryDto.Room room) {
        InquiryDto.Thread thread = new InquiryDto.Thread();
        thread.setRoomId(room.getRoomId());
        thread.setUnivId(room.getUnivId());
        thread.setUnivName(room.getUnivName());
        thread.setTitle(room.getTitle());
        thread.setCategory(room.getCategory());
        thread.setStatus(room.getStatus());
        thread.setCreatedAt(room.getCreatedAt());
        List<InquiryDto.Message> messages = inquiryMapper.selectMessages(room.getRoomId());
        messages.forEach(this::fillAttachments);
        thread.setMessages(messages);
        return thread;
    }

    private void fillAttachments(InquiryDto.Message message) {
        if (message == null) {
            return;
        }
        List<InquiryDto.Attachment> attachments =
                inquiryMapper.selectAttachmentsByMessageId(message.getMessageId());
        attachments.forEach(attachment -> attachment.setDownloadUrl(
                "/api/inquiries/attachments/" + attachment.getAttachmentId()
        ));
        message.setAttachments(attachments);
    }

    private void closeRoom(InquiryDto.Room room) {
        if (InquiryDto.STATUS_CLOSED.equals(room.getStatus())) {
            return;
        }
        inquiryMapper.updateRoomStatus(room.getRoomId(), InquiryDto.STATUS_CLOSED);
        room.setStatus(InquiryDto.STATUS_CLOSED);
        runAfterCommit(() -> messagingTemplate.convertAndSend(
                INQUIRY_TOPIC_PREFIX + room.getRoomId() + "/room",
                room
        ));
        runAfterCommit(() -> messagingTemplate.convertAndSend(INQUIRY_ROOMS_TOPIC, room));
    }

    private InquiryDto.Room requireRoomForDownload(MemberDto member, Long messageId) {
        InquiryDto.Message message = inquiryMapper.selectMessage(messageId);
        if (message == null) {
            return null;
        }
        InquiryDto.Room room = inquiryMapper.selectRoom(message.getRoomId(), member.getMemberId());
        if (room == null) {
            return null;
        }
        if (ROLE_SERVICE_ADMIN.equals(member.getRole())) {
            return room;
        }
        if (ROLE_ADMIN.equals(member.getRole()) && member.getUnivId() != null
                && member.getUnivId().equals(room.getUnivId())) {
            return room;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inquiry access denied.");
    }

    private InquiryDto.Room requireAdminRoom(Long roomId, Long univId) {
        InquiryDto.Room room = inquiryMapper.selectAdminRoom(roomId, univId, null);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inquiry room not found.");
        }
        return room;
    }

    private InquiryDto.Room requireRoom(Long roomId) {
        InquiryDto.Room room = inquiryMapper.selectRoom(roomId, null);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inquiry room not found.");
        }
        return room;
    }

    private MemberDto requireRole(Long memberId, String role) {
        MemberDto member = requireMember(memberId);
        if (!role.equals(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inquiry access denied.");
        }
        if (ROLE_ADMIN.equals(role) && member.getUnivId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin university is required.");
        }
        return member;
    }

    private MemberDto requireMember(Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login is required.");
        }
        MemberDto member = memberMapper.findByMemberId(memberId);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login is required.");
        }
        return member;
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required.");
        }
        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must be 50 characters or fewer.");
        }
    }

    private void validateMessage(String text, List<MultipartFile> files, boolean firstMessage) {
        boolean hasText = text != null && !text.trim().isEmpty();
        boolean hasFiles = !safeFiles(files).isEmpty();
        if (!hasText && !hasFiles) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    firstMessage ? "First message is required." : "Message or attachment is required.");
        }
        if (hasText && text.trim().length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must be 1000 characters or fewer.");
        }
        if (safeFiles(files).size() > MAX_FILES_PER_MESSAGE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Up to 3 files can be attached.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each attachment must be 20MB or smaller.");
        }
        String extension = getExtension(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported attachment type.");
        }
    }

    private String normalizeCategory(String category) {
        String value = category == null || category.isBlank()
                ? DEFAULT_CATEGORY
                : category.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_CATEGORIES.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported inquiry category.");
        }
        return value;
    }

    private List<MultipartFile> safeFiles(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
    }

    private String getExtension(String filename) {
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private void runAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
            return;
        }
        runnable.run();
    }
}
