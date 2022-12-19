package ru.job4j.grabber.utils;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class HabrCareerDateTimeParser implements DateTimeParser {

    @Override
    public LocalDateTime parse(String dateStr) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateStr);
        return zonedDateTime.toLocalDateTime();
    }

}