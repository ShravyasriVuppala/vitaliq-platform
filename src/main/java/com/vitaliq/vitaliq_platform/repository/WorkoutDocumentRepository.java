package com.vitaliq.vitaliq_platform.repository;

import com.vitaliq.vitaliq_platform.document.WorkoutDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutDocumentRepository extends ElasticsearchRepository<WorkoutDocument, UUID> {
    List<WorkoutDocument> findByUserId(UUID userId);
}