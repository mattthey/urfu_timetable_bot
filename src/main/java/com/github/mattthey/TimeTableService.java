package com.github.mattthey;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

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

    private static Elements getTimeTableRows(final String groupId) throws IOException
    {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final String date = dateFormat.format(new Date());
        final String urlForGetTimetable = String.format("https://urfu.ru/api/schedule/groups/lessons/%s/%s/",
                groupId, date);
        final Document document = Jsoup.connect(urlForGetTimetable).get();
        Element table = document.select("table").first(); //находим таблицу в документе
        return table.select("tr");// разбиваем нашу таблицу на строки по тегу
    }

    public static String getTimeTableForGroupIdOnTwoWeek(final String groupId) throws IOException
    {
        final Elements rows = getTimeTableRows(groupId);
        return getTimeTableByNDay(rows, rows.size());
    }

    public static String getTimeTableForGroupIdOnWeek(final String groupId) throws IOException
    {
        final Elements rows = getTimeTableRows(groupId);
        return getTimeTableByNDay(rows, 7);
    }

    public static String getTimeTableForGroupIdOnDay(final String groupId) throws IOException
    {
        Elements rows = getTimeTableRows(groupId);
        return getTimeTableByNDay(rows, 1);
    }

    private static String getTimeTableByNDay(final Elements rows, int countDay)
    {
        final StringBuilder sb = new StringBuilder();
        final Iterator<Element> iterator = rows.iterator();
        while (countDay > 0 && iterator.hasNext())
        {
            final Element element = iterator.next();
            final String elementText = element.text().trim();
            sb.append(elementText)
                    .append(System.lineSeparator());
            if (element.attr("class").equals("divide") && elementText.isEmpty())
            {
                countDay--;
            }
        }
        return sb.toString();
    }
}