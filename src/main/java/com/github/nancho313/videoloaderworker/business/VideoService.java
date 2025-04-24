package com.github.nancho313.videoloaderworker.business;

import com.github.nancho313.videoloaderworker.contract.messaging.dto.VideoToProcessMessage;
import com.github.nancho313.videoloaderworker.infrastructure.persistence.dao.VideoRepository;
import com.github.nancho313.videoloaderworker.infrastructure.s3.S3Integrator;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

@Service
public class VideoService {

  private final String folderDir;

  private final VideoRepository videoRepository;

  private final S3Integrator s3Integrator;

  public VideoService(@Value("${app.folder-dir}") String outputDir,
                      VideoRepository videoRepository, S3Integrator s3Integrator) {
    this.folderDir = outputDir;
    this.videoRepository = videoRepository;
    this.s3Integrator = s3Integrator;
  }

  public void processVideo(VideoToProcessMessage videoToProcess) {

    String inputFile = "";
    String outputFile = "";

    try {

      var video = videoRepository.findById(videoToProcess.videoId()).orElseThrow(() -> new NoSuchElementException("The video to process does not exist."));
      inputFile = s3Integrator.downloadVideo(video.getTitle());
      outputFile = addImageToVideo(inputFile);
      var processedUrl = s3Integrator.storeVideo(outputFile, video.getTitle());
      video.setProcessedUrl(processedUrl);
      video.setProcessedAt(LocalDateTime.now());
      video.setStatus("PROCESSED");
      videoRepository.save(video);

    } catch (Exception e) {

      e.printStackTrace();
    } finally {

      removeFile(inputFile);
      removeFile(outputFile);
    }
  }

  private String addImageToVideo(String inputFile) {

    String imagePath = folderDir + "/video_logo.png";
    String outputFile = folderDir + "/" + Instant.now().toEpochMilli() + ".mp4";
    FFmpegFrameGrabber grabber = null;
    FFmpegFrameRecorder recorder = null;
    OpenCVFrameConverter.ToMat converter = null;

    try {

      grabber = new FFmpegFrameGrabber(inputFile);
      grabber.start();

      recorder = new FFmpegFrameRecorder(
              outputFile,
              grabber.getImageWidth(),
              grabber.getImageHeight(),
              grabber.getAudioChannels()
      );
      recorder.setVideoCodec(grabber.getVideoCodec());
      recorder.setFormat("mp4");
      recorder.setFrameRate(grabber.getFrameRate());
      recorder.start();

      converter = new OpenCVFrameConverter.ToMat();
      Mat image = imread(imagePath);
      Mat resizedImage = new Mat();
      org.bytedeco.opencv.global.opencv_imgproc.resize(
              image, resizedImage, new Size(grabber.getImageWidth(), grabber.getImageHeight())
      );
      Frame imageFrame = converter.convert(resizedImage);

      // Add image at beginning (show it for 2 seconds)
      int numIntroFrames = (int) (grabber.getFrameRate() * 2);
      for (int i = 0; i < numIntroFrames; i++) {
        recorder.record(imageFrame);
      }

      // Add original video frames
      Frame frame;
      while ((frame = grabber.grabFrame()) != null) {
        recorder.record(frame);
      }

      // Add image at end (show it for 2 seconds)
      int numOutroFrames = (int) (grabber.getFrameRate() * 2);
      for (int i = 0; i < numOutroFrames; i++) {
        recorder.record(imageFrame);
      }

      grabber.stop();
      recorder.stop();

      return outputFile;
    } catch (Exception e) {

      throw new RuntimeException(e);
    } finally {

      if (converter != null) {
        converter.close();
      }

      if(recorder != null) {

        try {
          recorder.close();
        } catch (FrameRecorder.Exception e) {
          e.printStackTrace();
        }
      }

      if(grabber != null) {

        try {
          grabber.close();
        } catch (FrameGrabber.Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void removeFile(String file) {

    try {
      Files.delete(Path.of(URI.create("file:///"+file)));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
