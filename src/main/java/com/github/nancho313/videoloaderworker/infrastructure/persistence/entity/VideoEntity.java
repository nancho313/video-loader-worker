package com.github.nancho313.videoloaderworker.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity(name = "video_tbl")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoEntity {

  @Id
  private String id;
  private String userId;
  private String title;
  private String fileName;
  private String originalUrl;
  private String processedUrl;
  private String status;
  private LocalDateTime uploadedAt;
  private LocalDateTime processedAt;
}
