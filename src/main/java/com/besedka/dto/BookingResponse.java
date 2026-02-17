package com.besedka.dto;

import com.besedka.model.Booking;
import com.besedka.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record BookingResponse(
        Long id,
        Long cabinId,
        String cabinName,
        String cabinLocation,
        LocalDate date,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        BookingStatus status,
        Integer reminderBeforeMinutes,
        BigDecimal totalPrice) {

    public static BookingResponse from(Booking booking) {
        long hours = booking.getCheckOutTime().getHour() - booking.getCheckInTime().getHour();
        BigDecimal total = booking.getCabin().getPricePerHour().multiply(BigDecimal.valueOf(hours));
        return new BookingResponse(
                booking.getId(),
                booking.getCabin().getId(),
                booking.getCabin().getName(),
                booking.getCabin().getLocation(),
                booking.getDate(),
                booking.getCheckInTime(),
                booking.getCheckOutTime(),
                booking.getStatus(),
                booking.getReminderBeforeMinutes(),
                total
        );
    }
}
