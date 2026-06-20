package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 교수 강의 채팅 DTO 묶음 (3분류 네스티드).
 * <p>본체=CHAT_ROOM·CHAT_ROOM_MESSAGES·ROOM_MESSAGES_READ.
 * <p>※ 명명 규칙: DTO 변수명 = DB 컬럼명 카멜(자기/앵커 CHAT_ROOM_MESSAGES), 조인/파생/PK·ID는 의미 별칭 — 커뮤니티 PostDto 컨벤션.
 *    응답·WS payload JSON = 자기 컬럼 카멜(chtRomMsgContent·chtRomMsgDate). PK messageId·FK roomId·발신 senderLmsPrfId,
 *    조인 의미명(studentName·studentNo·courseName), 파생(lastMessage·lastAt·unread·sender·read·dateLabel)은 의미명 유지.
 *    요청 SendMessageReqDto.messageText는 write 플러밍(inbound)이라 유지(SLM-007 SubmitReqDto 선례).
 */
public final class LmsProfChatDto {

    private LmsProfChatDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatRoomResDto {
        private Long roomId;             // CHAT_ROOM.CHT_ROM_ID (PK → 의미명)
        private Long lecId;
        private Long studentLmsPrfId;    // CHAT_ROOM.LMS_PRF_ID (방 소유 학생 → 의미별칭)
        private Long professorLmsPrfId;  // LECTURE.LMS_PRF_ID (담당 교수 → 의미별칭)
        private String studentName;      // 학생 MEMBER_NAME (조인)
        private String studentNo;        // 학생 MEMBER.LOGIN_ID=학번 (조인 → 의미별칭)
        private String courseName;       // LECTURE_CODE.LEC_COD_NAME (조인)
        private Integer lecSection;      // LECTURE.LEC_SECTION
        private String lastMessage;      // 최근 메시지 미리보기 (서브쿼리 파생 → 의미명)
        private String lastAt;           // 최근 메시지 시각 (서브쿼리 파생 → 의미명)
        private Integer unread;          // 안읽음 카운트 (집계 파생 → 의미명)
        private Integer semYear;         // SEMESTERS.SEM_YEAR — '채팅 만들기'(startable) 년도/학기 필터용. 일반 목록은 미채움(null)
        private String semTerm;          // SEMESTERS.SEM_TERM — 학기 코드(공통코드 SEM_TERM). startable만 채움
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatMessageResDto {
        private Long messageId;          // CHAT_ROOM_MESSAGES.CHT_ROM_MSG_ID (PK → 의미명)
        private Long roomId;             // CHT_ROM_ID (FK → 의미명)
        private Long senderLmsPrfId;     // CHAT_ROOM_MESSAGES.LMS_PRF_ID (발신자 → 의미별칭)
        private String sender;           // 'me'/'student' (CASE 파생 → 의미명)
        private String chtRomMsgContent; // CHAT_ROOM_MESSAGES.CHT_ROM_MSG_CONTENT (자기 컬럼)
        private String chtRomMsgDate;    // CHAT_ROOM_MESSAGES.CHT_ROM_MSG_DATE ISO (자기 컬럼)
        private Boolean read;            // ROM_MESSAGES_READ.ROM_MSG_RED_YN (CASE 파생 → 의미명)
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatThreadResDto {
        private Long roomId;
        private String dateLabel;
        private List<ChatMessageResDto> messages;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendMessageReqDto {
        private String messageText;
    }
}
