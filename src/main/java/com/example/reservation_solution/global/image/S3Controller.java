package com.example.reservation_solution.global.image;

import com.example.reservation_solution.global.security.HostUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/host/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(@RequestBody PresignedUrlRequest request,
                                                                @AuthenticationPrincipal HostUserDetails userDetails) {
        log.info("get presigned url for file {}", request.getFileName());
        if (request.getFileName() == null || request.getContentType() == null) {
            return ResponseEntity.badRequest().build();
        }
        PresignedUrlResponse response = s3Service.generatePresignedUrl(
                request.getFileName(),
                request.getContentType()
        );
        return ResponseEntity.ok(response);
    }
}