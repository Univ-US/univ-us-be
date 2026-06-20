package com.univus.app.serviceadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class ServiceAdminBulkSignupDto {

    private ServiceAdminBulkSignupDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {
        private Long univId;
        private List<MemberInput> members;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MemberInput {
        private String loginId;
        private String password;
        private String memberName;
        private String phoneNumber;
        private String gender;
        private String birth;
        private String role;
        private Long deptId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Capacity {
        private Long univId;
        private String univName;
        private String planName;
        private Long maxMemberCount;
        private Long currentMemberCount;
    }

    @Getter
    @AllArgsConstructor
    public static class ValidationError {
        private final int rowNumber;
        private final String message;
    }

    @Getter
    @AllArgsConstructor
    public static class PrecheckResponse {
        private final boolean canRegister;
        private final String reason;
        private final String univName;
        private final String planName;
        private final long currentMemberCount;
        private final long maxMemberCount;
        private final int requestedMemberCount;
        private final long remainingMemberCount;
        private final List<ValidationError> errors;
    }

    @Getter
    @AllArgsConstructor
    public static class RegisterResponse {
        private final int createdCount;
    }
}
