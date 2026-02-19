package com.ssafy.withy.global.service; // 패키지 경로 확인!

import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 파일 업로드
     */
    public String uploadFile(MultipartFile file, String dirName) {
        if (file.isEmpty()) {
            throw new CustomException(GlobalErrorCode.FILE_NOT_FOUND);
        }

        String originalFilename = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String key = dirName + "/" + uuid + "_" + originalFilename;

        try (InputStream inputStream = file.getInputStream()) {
            // S3 업로드
            S3Resource resource = s3Template.upload(
                    bucket,
                    key,
                    inputStream,
                    ObjectMetadata.builder().contentType(file.getContentType()).build()
            );

            // 업로드 된 파일의 URL 반환
            return resource.getURL().toString();

        } catch (IOException e) {
            log.error("S3 Upload Error: {}", e.getMessage());
            throw new CustomException(GlobalErrorCode.S3_UPLOAD_FAILED);
        }
    }

    /**
     * 파일 삭제
     * S3 URL에서 Key만 추출하여 삭제 요청
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        try {
            // 1. URL 디코딩 (한글 파일명 등 대비)
            String decodedUrl = URLDecoder.decode(fileUrl, StandardCharsets.UTF_8);

            // 2. 키 추출 (bucket 이름 뒷부분부터가 Key)
            String splitStr = bucket + "/";
            int parsingIndex = decodedUrl.indexOf(splitStr);

            if (parsingIndex != -1) {
                String key = decodedUrl.substring(parsingIndex + splitStr.length());
                s3Template.deleteObject(bucket, key);
                log.info("S3 File Deleted: {}", key);
            } else {
                log.warn("S3 Delete Fail: URL parsing failed for {}", fileUrl);
            }

        } catch (Exception e) {
            log.error("S3 Delete Error: {}", e.getMessage());
            throw new CustomException(GlobalErrorCode.S3_DELETE_FAILED);
        }
    }
}