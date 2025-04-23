package com.github.nancho313.videoloaderworker.infrastructure.persistence.dao;

import com.github.nancho313.videoloaderworker.infrastructure.persistence.entity.VideoEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface VideoRepository extends CrudRepository<VideoEntity, String> {

  List<VideoEntity> findByUserId(String userId);

}
