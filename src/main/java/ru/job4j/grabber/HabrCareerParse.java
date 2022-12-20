package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HabrCareerParse implements Parse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    public static void main(String[] args) {
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        List<Post> posts = habrCareerParse.list(PAGE_LINK);
        for (Post post : posts) {
            System.out.println(post);
        }
    }

    private Document getDocument(String pageLink) {
        try {
            Connection connection = Jsoup.connect(pageLink);
            return connection.get();
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }
    }

    private String retrieveDescription(String link) {
        Document document = getDocument(link);
        Element descriptionElem = document.select(".style-ugc").first();
        return descriptionElem.text();
    }

    private Post getPost(Element vacancy) {
        Element titleElement = vacancy.select(".vacancy-card__title").first();
        String title = titleElement.text();
        String link = titleElement.child(0).attr("href");
        String description = retrieveDescription(SOURCE_LINK.concat(link));
        Element dateTimeElement = vacancy.select(".vacancy-card__date")
                .first().child(0);
        LocalDateTime created = dateTimeParser.parse(dateTimeElement.attr("datetime"));
        return new Post(title, link, description, created);
    }

    private List<Post> getPagePosts(String link) {
        Document document = getDocument(link);
        Elements vacancies = document.select(".vacancy-card__inner");
        return vacancies.stream().map(this::getPost).collect(Collectors.toList());
    }

    @Override
    public List<Post> list(String link) {
        List<Post> list = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            list.addAll(getPagePosts(
                    link.concat(String.format("?page=%d", i))
            ));
        }
        return list;
    }
}