package com.univus.app.cmypage.service;

import com.univus.app.cmypage.dto.CmypageProfileDto;
import com.univus.app.cmypage.dto.CmypageProfileUpdateDto;

public interface CmypagePolicy {

    CmypageProfileDto requireProfile(CmypageProfileDto profile);

    String getValidatedNickname(CmypageProfileUpdateDto request);

    void requireAvailableNickname(int duplicateCount);

    void requireDepartmentAvailable(
            boolean hasMemberDetail,
            Long defaultDeptId);

    void requireProfileUpdated(int updated);

    String normalizeTradeRole(String role);
}
