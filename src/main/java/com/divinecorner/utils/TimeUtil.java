package com.divinecorner.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Timestamps are stored in the server's zone (UTC inside Docker), but the shop runs on
 * Indian time. These helpers convert between the two so "today" means today in India.
 */
public final class TimeUtil {

    public static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private TimeUtil() {
    }

    /** Current shop-local date. */
    public static LocalDate shopToday() {
        return LocalDate.now(SHOP_ZONE);
    }

    /** Start of a shop-local day, expressed in server time (for DB queries). */
    public static LocalDateTime startOfShopDay(LocalDate shopDate) {
        return shopDate.atStartOfDay(SHOP_ZONE).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    /** Server timestamp -> shop-local timestamp. */
    public static LocalDateTime toShop(LocalDateTime serverTime) {
        if (serverTime == null) return null;
        return serverTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(SHOP_ZONE).toLocalDateTime();
    }

    public static LocalDate toShopDate(LocalDateTime serverTime) {
        LocalDateTime t = toShop(serverTime);
        return t != null ? t.toLocalDate() : null;
    }

    /** Server timestamp -> "yyyy-MM-ddTHH:mm:ss" in shop time (parsed by browsers as local time). */
    public static String format(LocalDateTime serverTime) {
        LocalDateTime t = toShop(serverTime);
        return t != null ? t.format(ISO) : null;
    }
}
