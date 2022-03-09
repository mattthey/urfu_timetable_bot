package com.github.mattthey;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Телеграм бот @timetable_urfu_bot для получения расписания
 */
public class TimetableUrfuBot extends TelegramLongPollingBot
{
    private static final String GET_TIMETABLE_DAY = "Получить расписание на сегодня";
    private static final String GET_TIMETABLE_FOR_WEEK = "Получить расписание на неделю";
    private static final String GET_TIMETABLE_FOR_TWO_WEEK = "Получить расписание на 2 недели";
    private static final String REMOVE_SETTINGS = "Удалить номер группы";
    private static final String GET_SETTINGS = "Получить номер группы";

    private static final Set<String> GET_TIMETABLE_COMMANDS = Set.of(
            GET_TIMETABLE_DAY,
            GET_TIMETABLE_FOR_WEEK,
            GET_TIMETABLE_FOR_TWO_WEEK
    );

    private final String botUsername;
    private final String botToken;

    public TimetableUrfuBot(String botUsername, String botToken)
    {
        this.botUsername = botUsername;
        this.botToken = botToken;
    }

    @Override
    public String getBotUsername()
    {
        return botUsername;
    }

    @Override
    public String getBotToken()
    {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update)
    {
        try
        {
            final Message message = update.getMessage();
            if (update.hasMessage() && message.hasText())
            {
                final String text = message.getText();
                // сначала проверяем не одна ли это из наших команд
                final SendMessage sendMessage = new SendMessage();
                if (GET_TIMETABLE_COMMANDS.contains(text))
                {
                    addTimetableInTextMessage(sendMessage, message);
                }
                else if (text.equals(GET_SETTINGS))
                {
                    addSettingsInTextMessage(sendMessage, message);
                }
                else if (text.equals(REMOVE_SETTINGS))
                {
                    removeSettings(sendMessage, message);
                }
                else
                {
                    // тогда ищем такую группу, если нашли, то добавляем в настройки и отправляем расписание
                    process(sendMessage, message);
                }
                sendMessage.setChatId(Long.toString(message.getChatId()));
                addMenuButton(sendMessage);
                execute(sendMessage);
            }
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
        }
    }

    private static void process(final SendMessage sendMessage, final Message message)
    {
        final String text = message.getText();
        final long groupId;
        try
        {
            groupId = TimeTableService.getGroupIdByTitle(text);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            sendMessage.setText("Извини, при получении id твоей группы возникли ошибки, свяжитесь с моими криворукими"
                    + " создателями.");
            return;
        }
        if (groupId == -1)
        {
            sendMessage.setText("Не нашлось группы с таким номером: " + text);
            return;
        }
        // если нашли все же группу, то сохраняем её в бд
        final long groupIdFromDb = H2DatabaseManager.getGroupIdByChatId(message.getChatId());
        if (groupIdFromDb == -1)
        {
            // не нашли группу, значит запись новая
            H2DatabaseManager.addSettings(message.getChatId(), groupId);
            addTextToSendMessage(sendMessage, "Кароче мы сохранили твои настройки.");
        }
        else
        {
            // нашли группу и обновляем запись
            H2DatabaseManager.updateSettings(groupId, message.getChatId());
            // сообщаем о том, что насттройки изменились
            addTextToSendMessage(sendMessage, "Кароче мы обновили твои настройки.");
        }
        // теперь отправляем желанное расписание
        addTimetableInTextMessage(sendMessage, message);
    }

    /**
     * Удалить настройки для текущего чата
     * @param sendMessage отправляемое сообщение
     * @param message полученное сообщение от пользователя
     */
    private static void removeSettings(final SendMessage sendMessage, final Message message)
    {
        if (H2DatabaseManager.removeSettingsByChatId(message.getChatId()))
        {
            addTextToSendMessage(sendMessage, "Мы успешно удалили твои настройки.");
        }
        else
        {
            addTextToSendMessage(sendMessage, "Мы не смогли удалить твои настройки или же их не запомнили/потеряли, ну что поделать"
                    + " мы же не почта России.");
        }
    }

    /**
     * Добавить к тексту сообщения id группы пользователя
     * @param sendMessage сообщение к которому надо добавить текст
     * @param message полученное сообщение от пользователя
     */
    private static void addSettingsInTextMessage(final SendMessage sendMessage, final Message message)
    {
        final long groupId = H2DatabaseManager.getGroupIdByChatId(message.getChatId());
        if (groupId == -1)
        {
            addTextToSendMessage(sendMessage, "Сорян, мы не нашли в своей памяти номер твоей группы попробуй отправить"
                    + " её еще раз, мы постараемся запомнить.");
            return;
        }
        addTextToSendMessage(sendMessage, "Id твоей группы " + groupId + ". Все нормально, если оно не совпадает с номером, я "
                + "просто решил её не хранить, ибо она вроде как не нужна. Ну на данный момент.");
    }

    /**
     * Добавить к тексту сообщения расписание
     * @param sendMessage сообщение к которому надо добавить текст
     * @param message полученное сообщение от пользователя
     */
    private static void addTimetableInTextMessage(final SendMessage sendMessage, final Message message)
    {
        final long groupId = H2DatabaseManager.getGroupIdByChatId(message.getChatId());
        if (groupId == -1)
        {
            addTextToSendMessage(sendMessage, "Сорян, мы не нашли в своей памяти номер твоей группы попробуй отправить её еще"
                    + " раз, мы постараемся запомнить.");
            return;
        }
        String timeTableForGroupId;
        try
        {
            final String text = message.getText();
            final String groupIdStr = Long.toString(groupId);
            timeTableForGroupId = switch (text)
            {
                case GET_TIMETABLE_DAY -> TimeTableService.getTimeTableForGroupIdOnDay(groupIdStr);
                case GET_TIMETABLE_FOR_WEEK -> TimeTableService.getTimeTableForGroupIdOnWeek(groupIdStr);
                default -> TimeTableService.getTimeTableForGroupIdOnTwoWeek(groupIdStr);
            };
        }
        catch (IOException e)
        {
            timeTableForGroupId = "Извини, возникли ошибки, свяжись с разрабами, они накосячили, они исправят.";
            e.printStackTrace();
        }
        addTextToSendMessage(sendMessage, timeTableForGroupId);
    }

    /**
     * Добавить меню к сообщению
     * @param sendMessage сообщение
     */
    private static void addMenuButton(final SendMessage sendMessage)
    {
        // создаем клавиатуру
        final ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        final KeyboardRow keyboardButtons = new KeyboardRow();
        keyboardButtons.add(GET_TIMETABLE_DAY);

        final KeyboardRow keyboardButtons1 = new KeyboardRow();
        keyboardButtons1.add(GET_TIMETABLE_FOR_WEEK);

        final KeyboardRow keyboardButtons2 = new KeyboardRow();
        keyboardButtons2.add(GET_TIMETABLE_FOR_TWO_WEEK);

        final KeyboardRow keyboardButtons3 = new KeyboardRow();
        keyboardButtons3.add(GET_SETTINGS);
        keyboardButtons3.add(REMOVE_SETTINGS);

        replyKeyboardMarkup.setKeyboard(List.of(keyboardButtons, keyboardButtons1, keyboardButtons2, keyboardButtons3));
        replyKeyboardMarkup.setResizeKeyboard(true);

        sendMessage.setReplyMarkup(replyKeyboardMarkup);
    }

    /**
     * Добавить к отправляемому сообщению новый текст.
     * @param sendMessage отправляемое сообщение
     * @param newText новый текст
     */
    private static void addTextToSendMessage(final SendMessage sendMessage, final String newText)
    {
        String textSendMessage = sendMessage.getText();
        if (textSendMessage == null)
        {
            textSendMessage = "";
        }
        textSendMessage += System.lineSeparator().repeat(2) + newText;
        sendMessage.setText(textSendMessage);
    }
}