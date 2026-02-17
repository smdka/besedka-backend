package com.besedka.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingRequest(
        @NotNull Long cabinId,
        @NotNull @FutureOrPresent LocalDate date,
        @NotNull LocalTime checkInTime,
        @NotNull LocalTime checkOutTime,
        Integer reminderBeforeMinutes) {
}
