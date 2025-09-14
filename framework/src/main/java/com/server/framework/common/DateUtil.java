package com.server.framework.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtil
{
    public static final String DATE_WITH_TIME_FORMAT = "dd/MM/yyyy hh : mm a";
    public static final ZoneId ASIA_KOLKATA_TIME_ZONE = ZoneId.of("Asia/Kolkata");
    public static final long ONE_SECOND_IN_MILLISECOND = 1000L;
    public static final long ONE_MINUTE_IN_MILLISECOND = 1000L * 60L;
    public static final long ONE_HOUR_IN_MILLISECOND = ONE_MINUTE_IN_MILLISECOND * 60L;
    public static final long ONE_DAY_IN_MILLISECOND = ONE_HOUR_IN_MILLISECOND * 24;

    public static long getDayStartInMillis(String date, String format) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern(format)).atStartOfDay(ASIA_KOLKATA_TIME_ZONE).toInstant().toEpochMilli();
    }

    public static long getDelayFromDayStart(int hour, int minute) {
        ZonedDateTime date = LocalDate.now().atStartOfDay(ASIA_KOLKATA_TIME_ZONE);
        long milliseconds = date.plusHours(hour).plusMinutes(minute).toInstant().toEpochMilli();
        return milliseconds - date.toInstant().toEpochMilli();
    }

    static long getDaysDiff(String format, String date1, String date2) {
        LocalDate localDate1 = LocalDate.parse(date1, DateTimeFormatter.ofPattern(format));
        LocalDate localDate2 = LocalDate.parse(date2, DateTimeFormatter.ofPattern(format));
        return ChronoUnit.DAYS.between(localDate1, localDate2);
    }

    public static long convertDateToMilliseconds(String date, String format) {
        return parse(date, format).toInstant().toEpochMilli();
    }

    public static String getFormattedCurrentTime() {
        return getFormattedTime(getCurrentTimeInMillis()).toUpperCase();
    }

    public static String getFormattedCurrentTime(String format) {
        return getFormattedTime(getCurrentTimeInMillis(), format);
    }

    public static String getFormattedTime(Long timeInMilliseconds) {
        return format(parse(timeInMilliseconds));
    }

    public static String getFormattedTime(Long timeInMilliseconds, String format) {
        return format(parse(timeInMilliseconds), format);
    }

    public static long getCurrentTimeInMillis() {
        return ZonedDateTime.now(ASIA_KOLKATA_TIME_ZONE).toInstant().toEpochMilli();
    }

    public static ZonedDateTime getCurrentTime() {
        return ZonedDateTime.now(ASIA_KOLKATA_TIME_ZONE);
    }

    public static ZonedDateTime parse(String date, String format) {
        LocalDateTime localDateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern(format));
        return convertLocalToZonedDateTime(localDateTime);
    }

    public static ZonedDateTime parse(Long timeInMilliseconds) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeInMilliseconds), ASIA_KOLKATA_TIME_ZONE);
        return convertLocalToZonedDateTime(localDateTime);
    }

    public static ZonedDateTime convertLocalToZonedDateTime(LocalDateTime localDateTime) {
        return ZonedDateTime.of(localDateTime, ASIA_KOLKATA_TIME_ZONE);
    }

    public static String format(ZonedDateTime zonedDateTime) {
        return format(zonedDateTime, DATE_WITH_TIME_FORMAT);
    }

    public static String format(ZonedDateTime zonedDateTime, String format) {
        return zonedDateTime.format(DateTimeFormatter.ofPattern(format));
    }
}
