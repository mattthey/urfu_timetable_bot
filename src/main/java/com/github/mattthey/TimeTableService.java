package com.github.mattthey;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 */
public class TimeTableService
{
    /**
     * Получить id группы по имени
     * @param groupTitle имя группы
     * @return id группы.
     */
    public static long getGroupIdByTitle(final String groupTitle) throws IOException
    {
        final String urlForGetGroup = "https://urfu.ru/api/schedule/groups/suggest/?query=" + groupTitle;
        final String jsonBody = Jsoup.connect(urlForGetGroup)
                .ignoreContentType(true)
                .execute()
                .body();
        final JSONObject jsonObject = new JSONObject(jsonBody);
        final JSONArray suggestions = jsonObject.getJSONArray("suggestions");
        if (suggestions.isEmpty())
        {
            System.out.println("Не нашлось группы с таким номером.");
            return -1;
        }
        final long groupId = (int)suggestions.getJSONObject(0).get("data");
        return groupId;
    }

    public static String getTimeTableForGroupId(final String groupId) throws IOException
    {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final String date = dateFormat.format(new Date());
        System.out.println("Current date = " + date);
        final String urlForGetTimetable = String.format("https://urfu.ru/api/schedule/groups/lessons/%s/%s/",
                groupId, date);
        final Document document = Jsoup.connect(urlForGetTimetable).get();

        Element table = document.select("table").first(); //находим таблицу в документе

        Elements rows = table.select("tr");// разбиваем нашу таблицу на строки по тегу

        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i); //по номеру индекса получает строку
            Elements cols = row.select("td");// разбиваем полученную строку по тегу  на столбы
            for (int j = 0; j < cols.size(); j++) {
                sb.append(cols.get(j).text());// столбец
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}