package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HabrCareerParse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    public static void main(String[] args) throws IOException, NullPointerException {
        HabrCareerParse habrCareerParse = new HabrCareerParse();
        System.out.println(habrCareerParse.retrieveDescription("https://career.habr.com/vacancies/1000108376"));
    }

    private Document getDocument(String pageLink) throws IOException {
        Connection connection = Jsoup.connect(pageLink);
        return connection.get();

    }

    private String getPageInfo(String pageLink) throws IOException, NullPointerException {
        Document document = getDocument(pageLink);
        Elements rows = document.select(".vacancy-card__inner");
        return rows.stream().map(row -> {
            Element titleElement = row.select(".vacancy-card__title").first();
            Element linkElement = titleElement.child(0);
            String vacancyName = titleElement.text();

            Element dateElement = row.select(".vacancy-card__date").first();
            Element dateTimeElement = dateElement.child(0);
            HabrCareerDateTimeParser parser = new HabrCareerDateTimeParser();
            LocalDateTime localDateTime = parser.parse(dateTimeElement.attr("datetime"));

            String vacancyLink = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
            return String.format(
                    "%s %s %s%n",
                    vacancyName,
                    vacancyLink,
                    localDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        }).reduce("", String::concat);
    }

    private String getPagesInfo(int count) throws IOException, NullPointerException {
        String rsl = "";
        for (int i = 1; i <= count; i++) {
            String link = PAGE_LINK.concat(String.format("?page=%d", i));
            rsl = rsl.concat(getPageInfo(link));
        }
        return rsl;
    }

    private String retrieveDescription(String link) throws IOException, NullPointerException {
        Document document = getDocument(link);
        Element descriptionText = document.select(".vacancy-description__text").first();
        Elements contents = descriptionText.select("p");
        return contents.stream().map(Element::text).reduce("", (total, text) -> total.concat(text).concat(System.lineSeparator()));
    }
}