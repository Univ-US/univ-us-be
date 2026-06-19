package com.univus.app.cmypage.service;

public interface CommunityAccountStatusService {

    void deactivateCommunity(Long memberId);

    void reactivateCommunity(Long memberId);
}
