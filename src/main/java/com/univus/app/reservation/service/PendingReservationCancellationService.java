package com.univus.app.reservation.service;

public interface PendingReservationCancellationService {

    void cancelAllPendingReservations(Long memberId);
}
