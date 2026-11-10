package com.hostflow.dto.request;

/** @deprecated Use {@link CompleteReservationOnboardingRequest}; payment is handled separately. */
@Deprecated
public record ConfirmReservationRequest(Long paymentMethodId) {}
