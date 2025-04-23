package com.github.nancho313.videoloaderworker.contract.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nancho313.videoloaderworker.business.VideoService;
import com.github.nancho313.videoloaderworker.contract.messaging.dto.VideoToProcessMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VideoToProcessConsumer {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final VideoService videoService;

  @SneakyThrows
  @RabbitListener(queues = "q-process-video")
  public void receiveMessage(String message) {

    videoService.processVideo(OBJECT_MAPPER.readValue(message, VideoToProcessMessage.class));
  }
}
