package org.art.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.art.dao.AppUserDAO;
import org.art.dao.RawDataDao;
import org.art.entity.AppUser;
import org.art.entity.RawData;
import org.art.entity.UserState;
import org.art.service.FileService;
import org.art.service.MainService;
import org.art.service.ProducerService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.art.entity.UserState.BASIC_STATE;
import static org.art.entity.UserState.WAIT_FOR_EMAIL_STATE;
import static org.art.service.enums.ServiceCommands.CANCEL;
import static org.art.service.enums.ServiceCommands.HELP;
import static org.art.service.enums.ServiceCommands.REGISTRATION;
import static org.art.service.enums.ServiceCommands.START;

@Log4j
@Service
@AllArgsConstructor
public class MainServiceImpl implements MainService {

    private final RawDataDao rawDataDao;
    private final ProducerService producerService;
    private final AppUserDAO appUserDAO;
    private final FileService fileService;

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);

        var appUser = findOrSaveAppUser(update);
        UserState userState = appUser.getState();
        String text = update.getMessage().getText();
        var output = "";

        if (CANCEL.equals(text)) {
            output = cancelProcess(appUser);
        } else if (BASIC_STATE.equals(userState)) {
            output = processServiceCommand(appUser, text);
        } else if (WAIT_FOR_EMAIL_STATE.equals(userState)) {
            // TODO: add email processing
        } else {
            log.error("Unknown user state: " + userState);
            output = "Unknown error! Enter /cancel and try again";
        }

        var chatId = update.getMessage().getChatId();
        sendAnswer(output, chatId);
    }

    @Override
    public void processDocMessage(Update update) {
        saveRawData(update);

        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();

        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }

        //TODO: add saving doc
        var answer = "Doc successful upload! Link to download https://test.ru/get-doc/42";
        sendAnswer(answer, chatId);
    }

    @Override
    public void processPhotoMessage(Update update) {
        saveRawData(update);

        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();

        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }

        //TODO: add saving photo
        var answer = "Photo successful upload! Link to download https://test.ru/get-photo/42";
        sendAnswer(answer, chatId);
    }

    private boolean isNotAllowToSendContent(Long chatId, AppUser appUser) {
        UserState state = appUser.getState();
        if (!appUser.getIsActive()) {
            var error = "Register ot activate your account for content uploading";
            sendAnswer(error, chatId);
            return true;
        } else if (!BASIC_STATE.equals(state)) {
            var error = "Cancel current command by /cancel for file uploading";
            sendAnswer(error, chatId);
            return true;
        }
        return false;
    }


    private void sendAnswer(String output, Long chatId) {
        var sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);

        producerService.producerAnswer(sendMessage);
    }

    private String processServiceCommand(AppUser appUser, String cmd) {
        if (REGISTRATION.equals(cmd)) {
            // TODO: add registration
            return "Temporarily unavailable!";
        } else if (HELP.equals(cmd)) {
            return help();
        } else if (START.equals(cmd)) {
            return "Hello! To see a list of available commands type /help";
        } else {
            return "Unknown command! To see a list of available commands type /help";
        }
    }

    private String help() {
        return "List of available commands:\n" +
                "/cancel - cancel of current command;\n" +
                "/registration - user registration;";
    }

    private String cancelProcess(AppUser appUser) {
        appUser.setState(BASIC_STATE);
        appUserDAO.save(appUser);
        return "Command cancelled!";
    }

    private void saveRawData(Update update) {
        var rawData = RawData.builder()
                .event(update)
                .build();

        rawDataDao.save(rawData);
    }

    private AppUser findOrSaveAppUser(Update update) {
        User telegramUser = update.getMessage().getFrom();

        AppUser persistentAppUser = appUserDAO.findAppUserByTelegramUserId(telegramUser.getId());

        if (persistentAppUser == null) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .username(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    // TODO change value after registration
                    .isActive(true)
                    .state(BASIC_STATE)
                    .build();

            return appUserDAO.save(transientAppUser);
        }

        return persistentAppUser;
    }
}
