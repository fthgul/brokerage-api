package com.midas.studycase.brokerageapi.service.producer;


import com.midas.studycase.brokerageapi.config.kafka.KafkaConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotifyProducerService {

    private final KafkaTemplate<String, String> notiyfKafkaTemplate;

    public NotifyProducerService(@Qualifier("notifyKafkaTemplate") KafkaTemplate<String, String> notiyfKafkaTemplate) {
        this.notiyfKafkaTemplate = notiyfKafkaTemplate;
    }

    /**
     * Sends a notification message to a specific user.
     *
     * @param userId  the ID of the user to be notified.
     * @param message the notification message.
     */
    public void notifyUser(Long userId, String message) {
        try {
            notiyfKafkaTemplate.send(KafkaConfig.USER_NOTIFICATION_TOPIC, String.valueOf(userId), message);
            log.info("Notification sent to user: {}. Message: {}", userId, message);
        } catch (Exception e) {
            log.error("Failed to send notification to user: {}. Message: {}", userId, message, e);
        }
    }
}

