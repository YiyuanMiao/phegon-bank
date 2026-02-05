package com.phegon.phegonbank.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j // 使用 Lombok 的日志注解
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        String originalFileName = file.getOriginalFilename();
        log.info("开始处理文件上传. 原始文件名: {}, 目标文件夹: {}", originalFileName, folderName);

        // 1. 严格处理后缀，确保没有双点号
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            String[] parts = originalFileName.split("\\.");
            fileExtension = "." + parts[parts.length - 1].replaceAll("[^a-zA-Z0-9]", "");
        } else {
            fileExtension = ".png"; // 默认后缀
        }

        // 2. 生成唯一的 S3 Key
        String newFileName = UUID.randomUUID().toString() + fileExtension;
        String s3Key = folderName + "/" + newFileName;

        log.info("生成的 S3 Key (路径): {}", s3Key);
        log.info("文件类型 (ContentType): {}, 文件大小: {} bytes", file.getContentType(), file.getSize());

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            log.info("正在向 S3 发送 PutObject 请求... 桶名: {}", bucketName);

            // 执行上传
            PutObjectResponse response = s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            log.info("S3 上传成功! 响应 ETag: {}", response.eTag());

            // 3. 获取并返回完整的 URL
            String fileUrl = s3Client.utilities()
                    .getUrl(builder -> builder.bucket(bucketName).key(s3Key))
                    .toString();

            log.info("最终生成的访问 URL: {}", fileUrl);
            return fileUrl;

        } catch (S3Exception e) {
            log.error("AWS S3 服务报错: 状态码 [{}], 错误代码 [{}], 错误信息 [{}]",
                    e.statusCode(), e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());
            throw new IOException("S3 上传失败: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("文件上传过程中发生非 S3 异常: ", e);
            throw new IOException("文件上传失败: " + e.getMessage(), e);
        }
    }

    public boolean deleteFile(String fileUrl) {
        log.info("尝试从 S3 删除文件: {}", fileUrl);
        try {
            // 从 URL 中解析 Key (排除域名部分)
            String bucketUrlPart = ".amazonaws.com/";
            if (!fileUrl.contains(bucketUrlPart)) {
                log.warn("URL 格式不正确，无法解析 S3 Key: {}", fileUrl);
                return false;
            }

            String s3Key = fileUrl.substring(fileUrl.indexOf(bucketUrlPart) + bucketUrlPart.length());
            log.info("解析出的待删除 Key: {}", s3Key);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("文件删除成功: {}", s3Key);
            return true;

        } catch (S3Exception e) {
            log.error("S3 删除操作失败: {}", e.awsErrorDetails().errorMessage());
            return false;
        } catch (Exception e) {
            log.error("解析或删除文件时发生意外错误: ", e);
            return false;
        }
    }
}