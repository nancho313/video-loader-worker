package com.github.nancho313.videoloaderworker.infrastructure.s3;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Component
public class S3Integrator {

  private final S3Client s3Client;

  private final String processedVideoBucket;

  private final String originalVideosBucket;

  private final String folderDir;

  public S3Integrator(@Value("${aws.access-key}") String accessKey,
                      @Value("${aws.secret-key}") String secretKey,
                      @Value("${aws.session-token}") String sessionToken,
                      @Value("${aws.processed-videos-bucket}") String processedVideoBucket,
                      @Value("${aws.original-videos-bucket}") String originalVideosBucket,
                      @Value("${app.folder-dir}") String folderDir) {

    this.processedVideoBucket = processedVideoBucket;
    this.originalVideosBucket = originalVideosBucket;
    this.folderDir = folderDir;
    AwsCredentials credentials = AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
    s3Client = S3Client
            .builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();
  }

  public String storeVideo(String video, String title) {

    RequestBody requestBody = RequestBody.fromFile(new File(video));
    PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(processedVideoBucket).key(title).build();
    s3Client.putObject(objectRequest, requestBody);
    var getUrlRequest = GetUrlRequest.builder().bucket(processedVideoBucket).key(title).build();
    var response = s3Client.utilities().getUrl(getUrlRequest);
    return response.toExternalForm();
  }

  @SneakyThrows
  public String downloadVideo(String title) {

    GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(originalVideosBucket).key(title).build();
    var response = s3Client.getObject(getObjectRequest);
    var temporalFileUrl = folderDir + "/"+ Instant.now().toEpochMilli() + ".mp4";
    Files.write(Path.of(URI.create("file:///"+temporalFileUrl)), response.readAllBytes());
    return temporalFileUrl;
  }

}
