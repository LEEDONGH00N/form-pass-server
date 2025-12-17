package com.example.reservation_solution.global.image;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresignedUrlResponse {
    private String presignedUrl; // S3로 직접 PUT 요청을 보낼 임시 URL
    private String fileUrl; // DB에 저장하고 웹에서 접근할 최종 S3 URL
}