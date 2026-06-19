package com.univus.app.serviceadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class ServiceAdminReservationPenaltyDto {

    private ServiceAdminReservationPenaltyDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PenaltySearch {
        private Long memberId;
        private String keyword;
        private String status;
        private Long univId;
        private int offset;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Penalty {
        private Long penaltyId;
        private Long memberId;
        private String memberName;
        private String loginId;
        private String penaltyType;
        private String reason;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime createdAt;
    }

    @Getter
    public static class PenaltyPage {
        private final List<Penalty> content;
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean first;
        private final boolean last;

        public PenaltyPage(List<Penalty> content, int page, int size, long totalElements) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = size <= 0 ? 0 : (int) ((totalElements + size - 1) / size);
            this.first = page <= 0;
            this.last = totalPages == 0 || page >= totalPages - 1;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class GrantRequest {
        @NotNull
        private Long memberId;
        @NotBlank
        private String reason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberPenaltyStatus {
        private Long memberId;
        private String memberName;
        private Integer activePenaltyCount;
        private Integer blockThreshold;
        private Boolean blocked;
    }
}
