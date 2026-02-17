package com.besedka.controller;

import com.besedka.dto.BookingRequest;
import com.besedka.dto.BookingResponse;
import com.besedka.model.Client;
import com.besedka.security.TelegramInitDataFilter;
import com.besedka.service.BookingService;
import com.besedka.service.ClientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final ClientService clientService;

    public BookingController(BookingService bookingService, ClientService clientService) {
        this.bookingService = bookingService;
        this.clientService = clientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody BookingRequest req, HttpServletRequest httpReq) {
        return bookingService.create(req, resolveClient(httpReq));
    }

    @GetMapping("/my")
    public List<BookingResponse> myBookings(HttpServletRequest httpReq) {
        return bookingService.getClientBookings(getTelegramUserId(httpReq));
    }

    private Client resolveClient(HttpServletRequest req) {
        return clientService.upsert(
                getTelegramUserId(req),
                (String) req.getAttribute(TelegramInitDataFilter.ATTR_FIRST_NAME),
                (String) req.getAttribute(TelegramInitDataFilter.ATTR_LAST_NAME),
                (String) req.getAttribute(TelegramInitDataFilter.ATTR_USERNAME));
    }

    private Long getTelegramUserId(HttpServletRequest req) {
        return (Long) req.getAttribute(TelegramInitDataFilter.ATTR_TELEGRAM_USER_ID);
    }
}
