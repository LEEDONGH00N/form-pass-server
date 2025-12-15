package com.example.reservation_solution.global.mail;

public interface MailService {
    void sendAuthEmail(String toEmail, String authCode);
}
