package pro.sky.telegrambot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class NotificationService {
    private final NotificationTaskRepository notificationTaskRepository;

    public NotificationService(NotificationTaskRepository notificationTaskRepository) {
        this.notificationTaskRepository = notificationTaskRepository;
    }

    public void save(NotificationTask notificationTask) {
        notificationTaskRepository.save(notificationTask);
    }

    public List<NotificationTask> findNotificationTaskByDateTimeEquals(LocalDateTime truncatedTo) {
        return notificationTaskRepository.findNotificationTaskByDateTimeEquals(truncatedTo);
    }
}
