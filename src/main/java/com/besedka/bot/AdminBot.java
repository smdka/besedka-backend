package com.besedka.bot;

import com.besedka.event.BookingCreatedEvent;
import com.besedka.event.BookingStatusChangedEvent;
import com.besedka.model.Booking;
import com.besedka.model.BookingStatus;
import com.besedka.model.Client;
import com.besedka.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class AdminBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(AdminBot.class);
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM", new Locale("ru"));

    private final String botUsername;
    private final String adminChannelId;
    private final BookingService bookingService;

    public AdminBot(
            @Value("${telegram.bot-token}") String botToken,
            @Value("${telegram.bot-username}") String botUsername,
            @Value("${telegram.admin-channel-id}") String adminChannelId,
            BookingService bookingService) {
        super(botToken);
        this.botUsername = botUsername;
        this.adminChannelId = adminChannelId;
        this.bookingService = bookingService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasCallbackQuery()) return;
        String data = update.getCallbackQuery().getData();
        if (data == null) return;

        String[] parts = data.split(":", 2);
        if (parts.length != 2) return;

        try {
            long bookingId = Long.parseLong(parts[1]);
            switch (parts[0]) {
                case "book_approve" -> bookingService.approve(bookingId);
                case "book_decline" -> bookingService.decline(bookingId);
            }
        } catch (Exception e) {
            log.error("Error handling callback '{}'", data, e);
        }
    }

    @EventListener
    public void onBookingCreated(BookingCreatedEvent event) {
        notifyNewBooking(event.getBooking());
    }

    @EventListener
    public void onBookingStatusChanged(BookingStatusChangedEvent event) {
        Booking booking = event.getBooking();
        switch (event.getNewStatus()) {
            case APPROVED -> {
                notifyClient(
                        booking.getClient().getTelegramUserId(),
                        "✅ Ваша заявка на " + booking.getCabin().getName() + " " + formatSlot(booking) + " одобрена!");
                resolveAdminMessage(booking, "✅ Одобрено");
            }
            case DECLINED -> {
                notifyClient(
                        booking.getClient().getTelegramUserId(),
                        "❌ К сожалению, ваша заявка на " + booking.getCabin().getName() + " " + formatSlot(booking) + " отклонена.");
                resolveAdminMessage(booking, "❌ Отклонено");
            }
            case CANCELLED -> resolveAdminMessage(booking, "🚫 Отменено клиентом");
        }
    }

    public void notifyNewBooking(Booking booking) {
        long hours = booking.getCheckOutTime().getHour() - booking.getCheckInTime().getHour();
        BigDecimal total = booking.getCabin().getPricePerHour().multiply(BigDecimal.valueOf(hours));

        Client client = booking.getClient();
        String clientDisplay = client.getUsername() != null
                ? "@" + client.getUsername() + " (" + fullName(client) + ")"
                : fullName(client);

        String reminderLine = (booking.getReminderBeforeMinutes() != null
                && booking.getReminderBeforeMinutes() > 0)
                ? "\n⏰ Напомнить за " + formatMinutes(booking.getReminderBeforeMinutes())
                : "";

        String text = "📅 *Новая заявка \\#" + booking.getId() + "*\n"
                + "🏠 " + escape(booking.getCabin().getName()) + "\n"
                + "📆 " + escape(booking.getDate().format(DATE_FMT))
                + " · " + escape(booking.getCheckInTime().toString()) + "–" + escape(booking.getCheckOutTime().toString())
                + " \\(" + hours + " ч\\)\n"
                + "💰 " + escape(total.toPlainString()) + " ₽\n"
                + "👤 " + escape(clientDisplay)
                + escape(reminderLine);

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text("✅ Одобрить")
                                .callbackData("book_approve:" + booking.getId())
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("❌ Отклонить")
                                .callbackData("book_decline:" + booking.getId())
                                .build()))
                .build();

        try {
            var sent = execute(SendMessage.builder()
                    .chatId(adminChannelId)
                    .text(text)
                    .parseMode(ParseMode.MARKDOWNV2)
                    .replyMarkup(keyboard)
                    .build());
            booking.setAdminMessageId(sent.getMessageId());
        } catch (TelegramApiException e) {
            log.error("Failed to notify admin channel for booking #{}", booking.getId(), e);
        }
    }

    public void notifyClient(Long telegramUserId, String text) {
        try {
            execute(SendMessage.builder()
                    .chatId(telegramUserId.toString())
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send DM to user {}", telegramUserId, e);
        }
    }

    public void resolveAdminMessage(Booking booking, String resultLabel) {
        if (booking.getAdminMessageId() == null) return;
        try {
            execute(EditMessageText.builder()
                    .chatId(adminChannelId)
                    .messageId(booking.getAdminMessageId())
                    .text(resultLabel + " — заявка #" + booking.getId()
                          + " (" + booking.getCabin().getName() + " " + booking.getDate() + ")")
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to edit admin message for booking #{}", booking.getId(), e);
        }
    }

    private String formatSlot(Booking b) {
        return b.getDate() + " · " + b.getCheckInTime() + "–" + b.getCheckOutTime();
    }

    private String fullName(Client c) {
        String fn = c.getFirstName() != null ? c.getFirstName() : "";
        String ln = c.getLastName()  != null ? c.getLastName()  : "";
        return (fn + " " + ln).trim();
    }

    private String formatMinutes(int minutes) {
        if (minutes < 60) return minutes + " минут";
        int h = minutes / 60;
        return h + " " + (h == 1 ? "час" : h < 5 ? "часа" : "часов");
    }

    private static String escape(String text) {
        return text.replaceAll("([_*\\[\\]()~`>#+\\-=|{}.!\\\\])", "\\\\$1");
    }
}
