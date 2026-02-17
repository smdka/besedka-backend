package com.besedka.config;

import com.besedka.bot.AdminBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    private static final Logger log = LoggerFactory.getLogger(BotConfig.class);

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        return new TelegramBotsApi(DefaultBotSession.class);
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> botRegistrar(TelegramBotsApi api, AdminBot bot) {
        return event -> {
            try {
                api.registerBot(bot);
                log.info("Telegram bot registered: @{}", bot.getBotUsername());
            } catch (TelegramApiException e) {
                log.warn("Telegram bot registration failed (running without bot): {}", e.getMessage());
            }
        };
    }
}
