package com.inventory.auth.mail;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(JavaMailSender.class)
public class LoggingMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailService.class);
    private static final int MAX_RECENT = 50;

    private final Deque<MailMessage> recentMessages = new ArrayDeque<>();

    @Override
    public synchronized void send(MailMessage message) {
        recentMessages.addFirst(message);
        while (recentMessages.size() > MAX_RECENT) {
            recentMessages.removeLast();
        }

        log.info(
            "Mail not configured; logging outbound message. to={} subject={} body={}",
            message.to(),
            message.subject(),
            message.body()
        );
    }

    public synchronized Optional<MailMessage> findLatestTo(String email) {
        return recentMessages.stream()
            .filter(message -> message.to().equalsIgnoreCase(email))
            .findFirst();
    }

    public synchronized void clear() {
        recentMessages.clear();
    }
}
