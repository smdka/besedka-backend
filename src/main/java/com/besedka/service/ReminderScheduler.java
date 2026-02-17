package com.besedka.service;

import com.besedka.bot.AdminBot;
import com.besedka.model.Booking;
import com.besedka.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final BookingRepository bookingRepository;
    private final AdminBot adminBot;

    public ReminderScheduler(BookingRepository bookingRepository, AdminBot adminBot) {
        this.bookingRepository = bookingRepository;
        this.adminBot = adminBot;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void sendReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);

        List<Booking> candidates = bookingRepository.findPendingReminders(today, now);

        for (Booking booking : candidates) {
            long minutesUntil = ChronoUnit.MINUTES.between(now, booking.getCheckInTime());
            if (minutesUntil <= booking.getReminderBeforeMinutes()) {
                sendReminder(booking, minutesUntil);
            }
        }
    }

    private void sendReminder(Booking booking, long minutesUntil) {
        String timeLabel = minutesUntil < 60
                ? "через " + minutesUntil + " " + minuteWord(minutesUntil)
                : "через " + (minutesUntil / 60) + " " + hourWord(minutesUntil / 60);

        String text = "⏰ Напоминание: " + timeLabel + " у вас бронирование в "
                + booking.getCabin().getName()
                + " (" + booking.getCheckInTime() + "–" + booking.getCheckOutTime() + ")";

        adminBot.notifyClient(booking.getClient().getTelegramUserId(), text);
        booking.setReminderSent(true);
        bookingRepository.save(booking);
        log.info("Sent reminder for booking #{}", booking.getId());
    }

    private String minuteWord(long n) {
        if (n >= 11 && n <= 19) return "минут";
        return switch ((int) (n % 10)) {
            case 1 -> "минуту";
            case 2, 3, 4 -> "минуты";
            default -> "минут";
        };
    }

    private String hourWord(long h) {
        if (h >= 11 && h <= 19) return "часов";
        return switch ((int) (h % 10)) {
            case 1 -> "час";
            case 2, 3, 4 -> "часа";
            default -> "часов";
        };
    }
}
