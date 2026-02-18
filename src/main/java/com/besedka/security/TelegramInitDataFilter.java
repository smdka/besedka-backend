package com.besedka.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TelegramInitDataFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TelegramInitDataFilter.class);

    public static final String ATTR_TELEGRAM_USER_ID = "telegramUserId";
    public static final String ATTR_FIRST_NAME       = "firstName";
    public static final String ATTR_LAST_NAME        = "lastName";
    public static final String ATTR_USERNAME         = "username";

    private final String botToken;
    private final boolean skipAuth;
    private final ObjectMapper objectMapper;

    public TelegramInitDataFilter(
            @Value("${telegram.bot-token}") String botToken,
            @Value("${telegram.auth.skip:false}") boolean skipAuth,
            ObjectMapper objectMapper) {
        this.botToken = botToken;
        this.skipAuth = skipAuth;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        String path = req.getRequestURI();
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        if (skipAuth) {
            req.setAttribute(ATTR_TELEGRAM_USER_ID, 0L);
            req.setAttribute(ATTR_FIRST_NAME, "Dev");
            req.setAttribute(ATTR_LAST_NAME, "User");
            req.setAttribute(ATTR_USERNAME, "devuser");
            chain.doFilter(request, response);
            return;
        }

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("tma ")) {
            ((HttpServletResponse) response).sendError(
                    HttpServletResponse.SC_UNAUTHORIZED, "Missing Telegram auth");
            return;
        }

        String initData = authHeader.substring(4);
        try {
            Map<String, String> params = parseInitData(initData);
            String hash = params.remove("hash");
            params.remove("signature");

            if (hash == null || !validateHash(params, hash)) {
                log.warn("initData hash mismatch, params keys: {}", params.keySet());
                ((HttpServletResponse) response).sendError(
                        HttpServletResponse.SC_UNAUTHORIZED, "Invalid initData signature");
                return;
            }

            String userJson = params.get("user");
            if (userJson != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> user = objectMapper.readValue(
                        URLDecoder.decode(userJson, StandardCharsets.UTF_8), Map.class);
                req.setAttribute(ATTR_TELEGRAM_USER_ID, Long.valueOf(user.get("id").toString()));
                req.setAttribute(ATTR_FIRST_NAME, String.valueOf(user.getOrDefault("first_name", "")));
                req.setAttribute(ATTR_LAST_NAME,  String.valueOf(user.getOrDefault("last_name",  "")));
                req.setAttribute(ATTR_USERNAME,   String.valueOf(user.getOrDefault("username",   "")));
            }
        } catch (Exception e) {
            log.error("Failed to validate Telegram initData: {}", e.getMessage());
            ((HttpServletResponse) response).sendError(
                    HttpServletResponse.SC_UNAUTHORIZED, "Invalid initData");
            return;
        }

        chain.doFilter(request, response);
    }

    private Map<String, String> parseInitData(String initData) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String part : initData.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) params.put(part.substring(0, eq), part.substring(eq + 1));
        }
        return params;
    }

    private boolean validateHash(Map<String, String> params, String hash) throws Exception {
        String dataCheckString = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] secretKey = mac.doFinal(botToken.getBytes(StandardCharsets.UTF_8));
        mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
        byte[] dataHash = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));

        return toHex(dataHash).equals(hash);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
