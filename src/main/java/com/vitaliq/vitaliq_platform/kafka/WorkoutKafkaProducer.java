package com.vitaliq.vitaliq_platform.kafka;

import com.vitaliq.vitaliq_platform.event.WorkoutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j  // generates logger field instead of manually declaring a logger
@Component
@RequiredArgsConstructor
public class WorkoutKafkaProducer {

    private static final String TOPIC = "workout-events";

    private final KafkaTemplate<String, WorkoutEvent> kafkaTemplate; //kafkaTemplate bean which spring initializes from app.yml configs

    public void  publish(WorkoutEvent event) {
        // Key = userId — all events for the same user go to the same partition
        // This guarantees ordering of workout events per user
        kafkaTemplate.send(TOPIC, event.getUserId().toString(), event);
        log.debug("Published WorkoutEvent to topic '{}' for workoutId={}",
                TOPIC, event.getWorkoutId());
    }
}