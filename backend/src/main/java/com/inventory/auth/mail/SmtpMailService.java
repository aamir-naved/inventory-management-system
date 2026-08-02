package com.inventory.auth.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.inventory.config.AuthProperties;

@Service
@ConditionalOnBean(JavaMailSender.class)
public class SmtpMailService implements MailService {

    private final JavaMailSender javaMailSender;
    private final AuthProperties authProperties;

    public SmtpMailService(JavaMailSender javaMailSender, AuthProperties authProperties) {
        this.javaMailSender = javaMailSender;
        this.authProperties = authProperties;
    }

    @Override
    public void send(MailMessage message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(authProperties.getMailFrom());
        mailMessage.setTo(message.to());
        mailMessage.setSubject(message.subject());
        mailMessage.setText(message.body());
        javaMailSender.send(mailMessage);
    }
}