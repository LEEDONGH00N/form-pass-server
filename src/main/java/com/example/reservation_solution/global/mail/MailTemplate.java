package com.example.reservation_solution.global.mail;

public class MailTemplate {
    public static final String EMAIL_TEMPLATE = """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>이메일 인증</title>
        </head>
        <body style="margin: 0; padding: 0; background-color: #f4f4f4; font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif;">
            <div style="max-width: 600px; margin: 0 auto; padding: 40px 0;">
                <div style="background-color: #ffffff; padding: 40px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); text-align: center;">
                    <h1 style="color: #6C63FF; font-size: 28px; margin-bottom: 30px; letter-spacing: -1px;">Form PASS</h1>
                    <h2 style="color: #333333; font-size: 24px; margin-bottom: 20px;">이메일 인증 코드를 확인해주세요</h2>
                    <p style="color: #666666; font-size: 16px; line-height: 1.6; margin-bottom: 30px;">
                        안녕하세요.<br>Form PASS 서비스 가입을 환영합니다.<br>아래 6자리 인증 코드를 입력하여 본인 인증을 완료해주세요.
                    </p>
                    <div style="background-color: #f8f9fa; padding: 20px; border-radius: 6px; margin-bottom: 30px; border: 1px solid #e9ecef;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #333;">%s</span>
                    </div>
                    <p style="color: #999999; font-size: 14px; margin-bottom: 40px;">* 이 인증 코드는 5분간 유효합니다.</p>
                    <hr style="border: 0; border-top: 1px solid #eeeeee; margin: 30px 0;">
                    <p style="color: #aaaaaa; font-size: 12px; line-height: 1.5;">&copy; 2025 Form PASS. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
        """;
}
