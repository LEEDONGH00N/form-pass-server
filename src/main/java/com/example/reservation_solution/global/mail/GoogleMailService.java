package com.example.reservation_solution.global.mail;

import com.example.reservation_solution.global.exception.EmailSendException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import static com.example.reservation_solution.global.mail.MailTemplate.EMAIL_TEMPLATE;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleMailService implements MailService {

    private final JavaMailSender javaMailSender;

    public void sendAuthEmail(String toEmail, String authCode) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("[Form PASS] 회원가입 인증 코드 안내");
            String content = String.format(EMAIL_TEMPLATE, authCode);
            helper.setText(content, true);
            javaMailSender.send(mimeMessage);
            log.info("인증 메일 발송 성공: {}", toEmail);

        } catch (MessagingException e) {
            log.error("메일 발송 실패: {}", toEmail, e);
            throw new EmailSendException("이메일 발송 중 오류가 발생했습니다.", e);
        }
    }
}