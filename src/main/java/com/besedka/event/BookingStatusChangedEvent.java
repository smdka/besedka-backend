package com.besedka.event;

import com.besedka.model.Booking;
import com.besedka.model.BookingStatus;
import org.springframework.context.ApplicationEvent;

public class BookingStatusChangedEvent extends ApplicationEvent {

    private final Booking booking;
    private final BookingStatus newStatus;

    public BookingStatusChangedEvent(Object source, Booking booking, BookingStatus newStatus) {
        super(source);
        this.booking = booking;
        this.newStatus = newStatus;
    }

    public Booking getBooking() {
        return booking;
    }

    public BookingStatus getNewStatus() {
        return newStatus;
    }
}
