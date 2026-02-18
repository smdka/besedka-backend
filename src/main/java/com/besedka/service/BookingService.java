package com.besedka.service;

import com.besedka.dto.BookingRequest;
import com.besedka.dto.BookingResponse;
import com.besedka.event.BookingCreatedEvent;
import com.besedka.event.BookingStatusChangedEvent;
import com.besedka.model.*;
import com.besedka.repository.BookingRepository;
import com.besedka.repository.CabinRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CabinRepository cabinRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(BookingRepository bookingRepository,
                          CabinRepository cabinRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.cabinRepository = cabinRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public BookingResponse create(BookingRequest req, Client client) {
        Cabin cabin = cabinRepository.findById(req.cabinId())
                .filter(Cabin::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cabin not found"));

        if (!req.checkInTime().isBefore(req.checkOutTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Check-out must be after check-in");
        }

        if (bookingRepository.existsConflict(cabin.getId(), req.date(), req.checkInTime(), req.checkOutTime())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cabin already booked for this time slot");
        }

        Booking booking = new Booking();
        booking.setCabin(cabin);
        booking.setClient(client);
        booking.setDate(req.date());
        booking.setCheckInTime(req.checkInTime());
        booking.setCheckOutTime(req.checkOutTime());
        booking.setReminderBeforeMinutes(req.reminderBeforeMinutes());
        booking = bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingCreatedEvent(this, booking));

        return BookingResponse.from(booking);
    }

    @Transactional
    public void approve(Long bookingId) {
        Booking booking = findPending(bookingId);
        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);
        eventPublisher.publishEvent(new BookingStatusChangedEvent(this, booking, BookingStatus.APPROVED));
    }

    @Transactional
    public void decline(Long bookingId) {
        Booking booking = findPending(bookingId);
        booking.setStatus(BookingStatus.DECLINED);
        bookingRepository.save(booking);
        eventPublisher.publishEvent(new BookingStatusChangedEvent(this, booking, BookingStatus.DECLINED));
    }

    @Transactional
    public void cancel(Long bookingId, Long telegramUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!booking.getClient().getTelegramUserId().equals(telegramUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        eventPublisher.publishEvent(new BookingStatusChangedEvent(this, booking, BookingStatus.CANCELLED));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getClientBookings(Long telegramUserId) {
        return bookingRepository.findByClientTelegramUserId(telegramUserId)
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    private Booking findPending(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking is no longer pending");
        }
        return booking;
    }
}
