package pro.sky.telegrambot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pro.sky.telegrambot.config.TelegramBotConfig;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private NotificationTaskRepository notificationTaskRepository;
    final TelegramBotConfig telegramBotConfig;
    private Pattern pattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}) (.+)");

    public TelegramBot(TelegramBotConfig telegramBotConfig, NotificationTaskRepository notificationTaskRepository) {
        this.telegramBotConfig = telegramBotConfig;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome massage"));
        listOfCommands.add(new BotCommand("/instruction", "get instruction"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error settings bot's commandlist: " + e.getMessage());
        }
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.info("Was invoked method for onUpdateReceived");
        if (update.hasMessage() && update.getMessage().hasText()) {
            log.info("onUpdateReceived");
            String massageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            Matcher matcher = pattern.matcher(massageText);
            if (matcher.matches()) {
                String dateTimeString = matcher.group(1);
                String notificationText = matcher.group(2);

                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, dateTimeFormatter);
                NotificationTask notificationTask = new NotificationTask(chatId, notificationText, dateTime);
                notificationTaskRepository.save(notificationTask);
                sendMassage(chatId, "Reminder added!");
            } else {
                switch (massageText) {
                    case "/start":
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/instruction":
                        sendMassage(chatId, "Enter the task in the format: (date)dd.mm.yyyy (time)hh:mm notification task. If you entered everything correctly, you will receive a message that a reminder has been added");
                        break;
                    default:
                        sendMassage(chatId, "Sorry, command was not recognized");
                }
            }
        }
    }

    @Override
    public String getBotToken() {
        log.info("Was invoked method for getBotToken");
        return telegramBotConfig.getToken();
    }

    @Override
    public String getBotUsername() {
        log.info("Was invoked method for getBotUsername");
        return telegramBotConfig.getBotName();
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Hi, " + name + ", nice to meet you!";
        log.info("Replied to user: " + name);

        sendMassage(chatId, answer);
    }

    private void sendMassage(Long chatId, String massage) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(massage);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void checker() {
        log.info("Was invoked method for checker");
        List<NotificationTask> currentNotifications = notificationTaskRepository.findNotificationTaskByDateTimeEquals(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        sendNotify(currentNotifications);
    }

    public void sendNotify(List<NotificationTask> currentNotifications) {
        currentNotifications.forEach(notificationTask -> {
            if (notificationTask == null) {
            } else {
                sendMassage(notificationTask.getChatId(), notificationTask.getNotification());
            }
        });
    }
}
