package com.univus.app.serviceadmin.service;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.serviceadmin.dto.ServiceAdminSchoolDto;
import com.univus.app.serviceadmin.mapper.ServiceAdminSchoolMapper;
import com.univus.app.subscription.service.SubscriptionLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceAdminSchoolService {

    private static final int PAGE_SIZE = 10;
    private static final Set<String> SUBSCRIPTION_STATUSES = Set.of(
            "ACTIVE", "PAST_DUE", "PENDING", "CANCELED", "UNSUBSCRIBED"
    );
    private static final Set<String> SORT_OPTIONS = Set.of(
            "NAME_ASC", "MEMBERS_ASC", "MEMBERS_DESC", "REVENUE_ASC", "REVENUE_DESC"
    );

    private final ServiceAdminSchoolMapper serviceAdminSchoolMapper;
    private final SubscriptionLifecycleService subscriptionLifecycleService;

    public PaginateUtilRestApiRes<ServiceAdminSchoolDto.School> getSchools(
            int page,
            String keyword,
            String subscriptionStatus,
            Long planId,
            String sort
    ) {
        int safePage = Math.max(page, 0);
        ServiceAdminSchoolDto.Search search = new ServiceAdminSchoolDto.Search();
        search.setKeyword(normalizeKeyword(keyword));
        search.setSubscriptionStatus(normalizeSubscriptionStatus(subscriptionStatus));
        search.setPlanId(planId);
        search.setSort(normalizeSort(sort));
        search.setOffset(safePage * PAGE_SIZE);

        List<ServiceAdminSchoolDto.School> schools =
                serviceAdminSchoolMapper.selectSchools(search);
        attachAdmins(schools);

        return new PaginateUtilRestApiRes<>(
                schools,
                safePage,
                PAGE_SIZE,
                serviceAdminSchoolMapper.countSchools(search)
        );
    }

    public ServiceAdminSchoolDto.School getSchool(Long univId) {
        ServiceAdminSchoolDto.School school =
                serviceAdminSchoolMapper.selectSchoolById(univId);
        if (school == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "학교를 찾을 수 없습니다.");
        }
        school.setAdmins(serviceAdminSchoolMapper.selectAdminsByUniversityId(univId));
        return school;
    }

    public ServiceAdminSchoolDto.School changePlan(Long univId, Long planId) {
        subscriptionLifecycleService.changePlan(univId, planId);
        return getSchool(univId);
    }

    public ServiceAdminSchoolDto.School scheduleCancellation(Long univId) {
        subscriptionLifecycleService.scheduleCancellation(univId);
        return getSchool(univId);
    }

    private void attachAdmins(List<ServiceAdminSchoolDto.School> schools) {
        if (schools.isEmpty()) {
            return;
        }

        List<Long> univIds = schools.stream()
                .map(ServiceAdminSchoolDto.School::getUnivId)
                .toList();
        Map<Long, List<ServiceAdminSchoolDto.Admin>> adminsByUniversity =
                serviceAdminSchoolMapper.selectAdminsByUniversityIds(univIds)
                        .stream()
                        .collect(Collectors.groupingBy(ServiceAdminSchoolDto.Admin::getUnivId));

        schools.forEach(school -> school.setAdmins(
                adminsByUniversity.getOrDefault(school.getUnivId(), List.of())
        ));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSubscriptionStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!SUBSCRIPTION_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 구독 상태입니다."
            );
        }
        return normalized;
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "NAME_ASC";
        }
        String normalized = sort.trim().toUpperCase();
        if (!SORT_OPTIONS.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 정렬 방식입니다."
            );
        }
        return normalized;
    }
}
