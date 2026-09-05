package com.inventory.auth.sms;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.inventory.config.AuthProperties;

@Service
public class SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(SmsGateway.class);
    private static final int MAX_RECENT = 50;

    private final AuthProperties authProperties;
    private final RestClient restClient = RestClient.create();
    private final Deque<SmsMessage> recentMessages = new ArrayDeque<>();

    public SmsGateway(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public synchronized void send(String to, String body) {
        SmsMessage message = new SmsMessage(to, body);
        recentMessages.addFirst(message);
        while (recentMessages.size() > MAX_RECENT) {
            recentMessages.removeLast();
        }

        String webhook = authProperties.getSmsWebhookUrl();
        if (webhook == null || webhook.isBlank()) {
            log.info("SMS not configured; logging OTP message. to={} body={}", to, body);
            return;
        }

        restClient.post()
            .uri(webhook.trim())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("to", to, "body", body))
            .retrieve()
            .toBodilessEntity();
    }

    public synchronized Optional<SmsMessage> findLatestTo(String phone) {
        return recentMessages.stream()
            .filter(message -> message.to().equals(phone))
            .findFirst();
    }

    public synchronized void clear() {
        recentMessages.clear();
    }
}
