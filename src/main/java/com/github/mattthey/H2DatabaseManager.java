package com.github.mattthey;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;

/**
 * Менеджер БД H2
 */
public class H2DatabaseManager
{
    private static final Path DIRECTORY_DB;
    private static final String JDBC_URL;
    private static final String JDBC_USERNAME = "tgbot";
    private static final String JDBC_PASSWORD = "tgbot";

    private static final String INIT_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS settings (
                chatId BIGINT PRIMARY KEY,
                groupId BIGINT
            )""";
    private static final String INSERT_SETTINGS_SQL = "INSERT INTO settings(chatId, groupId) VALUES (?, ?)";
    private static final String EDIT_SETTINGS_SQL = "UPDATE settings SET groupId = ? WHERE chatId = ?";
    private static final String SELECT_GROUP_ID_BY_CHAT_ID = "SELECT groupId FROM settings WHERE chatId = ?";
    private static final String REMOVE_BY_CHAT_ID = "DELETE FROM settings WHERE chatId = ?";

    private static final JdbcConnectionPool connectionPool;

    static
    {
        final JdbcDataSource jdbcDataSource = new JdbcDataSource();

        try
        {
            final Path h2DatabaseDir = Path.of("h2-database");
            DIRECTORY_DB = Files.exists(h2DatabaseDir) ?
                    h2DatabaseDir
                    : Files.createDirectory(h2DatabaseDir);
            JDBC_URL = "jdbc:h2:" + DIRECTORY_DB.toAbsolutePath() + "/tgbot";

            jdbcDataSource.setUrl(JDBC_URL);
            jdbcDataSource.setUser(JDBC_USERNAME);
            jdbcDataSource.setPassword(JDBC_PASSWORD);

            connectionPool = JdbcConnectionPool.create(jdbcDataSource);
            initTable();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Инициализация таблицы с настройками
     * @throws SQLException ошибка при выполнении sql
     */
    private static void initTable() throws SQLException
    {
        try (final Connection connection = connectionPool.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(INIT_TABLE_SQL))
        {
            preparedStatement.execute();
        }
    }

    /**
     * Добавить настройки для чата
     * @param chatId id чата для которого добавляем настройки
     * @param groupId id группы для которого добавляем настройки
     * @return успешно ли выполнилась операция
     */
    public static boolean addSettings(final long chatId, final long groupId)
    {
        try (final Connection connection = connectionPool.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SETTINGS_SQL))
        {
            preparedStatement.setLong(1, chatId);
            preparedStatement.setLong(2, groupId);
            preparedStatement.execute();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Получить ID группы из настроек, если они лежат в БД, то вернется id группы, если ничего не нашлось, то -1
     * @param chatId id чата с которым общаемся
     * @return результат
     */
    public static long getGroupIdByChatId(final long chatId)
    {
        try (final Connection connection = connectionPool.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(SELECT_GROUP_ID_BY_CHAT_ID))
        {
            preparedStatement.setLong(1, chatId);
            final ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next())
            {
                return resultSet.getLong("groupId");
            }
            return -1;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Удалить настройки для чата
     * @param chatId id чата для которого удаляем настройки
     * @return успешно ли выполнилаь операция
     */
    public static boolean removeSettingsByChatId(final long chatId)
    {
        try (final Connection connection = connectionPool.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(REMOVE_BY_CHAT_ID))
        {
            preparedStatement.setLong(1, chatId);
            return preparedStatement.executeUpdate() != 0;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Обновить таблицу настроек, изменить группу для пользователя
     * @param groupId id группы
     * @param chatId id чата
     */
    public static void updateSettings(final long groupId, final long chatId)
    {
        try (final Connection connection = connectionPool.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(EDIT_SETTINGS_SQL))
        {
            preparedStatement.setLong(1, groupId);
            preparedStatement.setLong(2, chatId);
            preparedStatement.execute();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}