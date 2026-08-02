package com.inventory.auth.mail;

public record MailMessage(
    String to,
    String subject,
    String body
) {
}
