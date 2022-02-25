package com.github.mattthey;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.sun.net.httpserver.HttpServer;

/**
 *
 */
public class Main
{
    private static final String BOT_DESCRIPTION = "I will get everything I want.";
    public static void main(String[] args) throws TelegramApiException, IOException, SQLException
    {
        final String botUsername = System.getenv("botUsername");
        final String botToken = System.getenv("botToken");
        if (botUsername == null || botToken == null)
        {
            throw new RuntimeException("Не установлен botUsername или botToken.");
        }
        startBot(botUsername, botToken);
        startHttpServer();
    }

    private static void startBot(final String botUsername, final String botToken) throws TelegramApiException, IOException
    {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        final TimetableUrfuBot matHelperBot = new TimetableUrfuBot(botUsername, botToken);
        telegramBotsApi.registerBot(matHelperBot);
    }

    /**
     * Для деплоя на heroku это необходимо.
     */
    private static void startHttpServer()
    {
        final int port = Integer.parseInt(System.getenv("PORT"));
        final HttpServer httpServer;
        try
        {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/", exchange ->
            {
                System.out.println("Update date " + new Date());
                exchange.sendResponseHeaders(200, BOT_DESCRIPTION.length());
                OutputStream os = exchange.getResponseBody();
                os.write(BOT_DESCRIPTION.getBytes());
                os.close();
            });
            httpServer.start();
            System.out.printf("Http server start on port %d.\n", port);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}