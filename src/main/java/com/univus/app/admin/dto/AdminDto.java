package com.univus.app.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class AdminDto {

    @Data
    public static class MemberListDto {
        private Long memberId;
        private Long univId;
        private String univName;
        private String schoolPhone;
        private String address;
        private Long deptId;
        private String memberName;
        private String role;
        private Long phoneNumber;
        private String gender;
        private String status;
        private LocalDateTime createdAt;
        private String communityNickname;
    }

    @Data
    public static class MemberSearchDto {
        private Long memberId;
        private String memberName;
        private Long deptId;
        private Long univId;
        private int page;
        private int size;
    }

    @Data
    public static class MemberItemDto {
        private Long memberId;
        private String password;
        private String memberName;
        private String role;
        private Long phoneNumber;
        private String gender;
        private String birth;
        private Long deptId;
        private String communityNickname;
    }

    @Data
    public static class MemberBulkRequestDto {
        private List<MemberItemDto> members;
    }

    @Data
    public static class MemberStatusDto {
        private Long memberId;
        private String status;
    }

    @Data
    public static class NoticeDto {
        private Long noticeId;
        private Long memberId;
        private Long univId;
        private String title;
        private String content;
    }

    @Data
    public static class NoticeListDto {
        private Long noticeId;
        private Long memberId;
        private Long univId;
        private String title;
        private LocalDateTime postedAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class SupportRequestDto {
        private Long memberId;
        private Long univId;
        private String memberName;
        private String contact;
        private String message;
    }

    @Data
    public static class SupportListDto {
        private Long supportId;
        private Long memberId;
        private Long univId;
        private String univName;
        private String memberName;
        private String contact;
        private String message;
        private LocalDateTime createdAt;
    }

    @Data
    public static class SupportStatusDto {
        private Long supportId;
        private Integer status;
    }

    @Data
    public static class UniversityDto {
        private Long univId;
        private String univName;
        private String schoolPhone;
        private String homepage;
        private String address;
    }
}
