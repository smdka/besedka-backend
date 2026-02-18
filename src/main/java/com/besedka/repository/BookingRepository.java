package com.besedka.repository;

import com.besedka.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END FROM Booking b
            WHERE b.cabin.id = :cabinId
              AND b.date = :date
              AND b.status IN (com.besedka.model.BookingStatus.PENDING, com.besedka.model.BookingStatus.APPROVED)
              AND b.checkInTime < :checkOut
              AND b.checkOutTime > :checkIn
            """)
    boolean existsConflict(@Param("cabinId") Long cabinId,
                           @Param("date") LocalDate date,
                           @Param("checkIn") LocalTime checkIn,
                           @Param("checkOut") LocalTime checkOut);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.cabin
            WHERE b.client.telegramUserId = :telegramUserId
            ORDER BY b.date DESC, b.checkInTime DESC
            """)
    List<Booking> findByClientTelegramUserId(@Param("telegramUserId") Long telegramUserId);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.cabin
            JOIN FETCH b.client
            WHERE b.status = com.besedka.model.BookingStatus.APPROVED
              AND b.reminderBeforeMinutes IS NOT NULL
              AND b.reminderBeforeMinutes > 0
              AND b.reminderSent = false
              AND b.date = :today
              AND b.checkInTime > :now
            """)
    List<Booking> findPendingReminders(@Param("today") LocalDate today,
                                       @Param("now") LocalTime now);
}
