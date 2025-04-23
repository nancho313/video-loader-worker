package com.github.nancho313.videoloaderworker.business;

import com.github.nancho313.videoloaderworker.contract.messaging.dto.VideoToProcessMessage;
import com.github.nancho313.videoloaderworker.infrastructure.persistence.dao.VideoRepository;
import com.github.nancho313.videoloaderworker.infrastructure.persistence.entity.VideoEntity;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

@Service
public class VideoService {

  private final String folderDir;

  private final String serviceEndpoint;

  private final VideoRepository videoRepository;

  public VideoService(@Value("${app.folder-dir}") String outputDir,
                      @Value("${app.service-endpoint}") String serviceEndpoint,
                      VideoRepository videoRepository) {
    this.folderDir = outputDir;
    this.serviceEndpoint = serviceEndpoint;
    this.videoRepository = videoRepository;
  }

  public void processVideo(VideoToProcessMessage videoToProcess) {

    var video = videoRepository.findById(videoToProcess.videoId()).orElseThrow(() -> new NoSuchElementException("The video to process does not exist."));
    addImageToVideo(video);

    video.setProcessedAt(LocalDateTime.now());
    video.setStatus("PROCESSED");
    video.setProcessedUrl(serviceEndpoint+"/processed/"+video.getFileName());
    videoRepository.save(video);
  }

  private void addImageToVideo(VideoEntity video) {

    String inputVideoPath = folderDir + "/original/" + video.getFileName();
    String outputVideoPath = folderDir + "/processed/"+ video.getFileName();
    String imagePath = folderDir + "/video_logo.png";

    FFmpegFrameGrabber grabber = null;
    FFmpegFrameRecorder recorder = null;
    OpenCVFrameConverter.ToMat converter = null;

    try {

      grabber = new FFmpegFrameGrabber(inputVideoPath);
      grabber.start();

      recorder = new FFmpegFrameRecorder(
              outputVideoPath,
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
}
