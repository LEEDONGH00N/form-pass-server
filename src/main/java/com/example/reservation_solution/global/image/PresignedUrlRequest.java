package com.example.reservation_solution.global.image;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PresignedUrlRequest {
    private String fileName;
    private String contentType;
}