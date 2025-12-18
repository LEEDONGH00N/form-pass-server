package com.example.reservation_solution.global.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region.s3}")
    private String region;

    public S3Service(S3Presigner s3Presigner) {
        this.s3Presigner = s3Presigner;
    }


    public PresignedUrlResponse generatePresignedUrl(String originalFileName, String contentType) {
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        log.info("Generating Presigned URL for Key: {}, ContentType: {}", uniqueFileName, contentType);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueFileName)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5)) // 5분 동안만 유효
                .putObjectRequest(putObjectRequest)
                .build();

        // 4. URL 생성 실행 및 결과 획득
        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);
        String presignedUrl = presignedPutObjectRequest.url().toString();

        // 5. 최종 S3 URL 생성 (DB 저장 및 웹에서 사용될 URL)
        String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, uniqueFileName);

        return new PresignedUrlResponse(presignedUrl, fileUrl);
    }
}
