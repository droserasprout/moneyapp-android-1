package com.cactusteam.money.data.period;

import android.util.Pair;

import java.util.Calendar;
import java.util.Date;

/**
 * @author vpotapenko
 */
public class Period {

    public static final int MONTH_TYPE = 0;
    public static final int WEEK_TYPE = 1;

    public final int type;
    public final int startFrom;

    private IPeriodWalker periodWalker;

    public Period(int type, int startFrom) {
        this.type = type;
        this.startFrom = startFrom;
    }

    public Period(String serializeString) {
        String[] parts = serializeString.split(":");

        int typeValue = MONTH_TYPE;
        try {
            typeValue = Integer.parseInt(parts[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        type = typeValue;

        int startValue = type == MONTH_TYPE ? 1 : Calendar.getInstance().getFirstDayOfWeek();
        try {
            startValue = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        startFrom = startValue;
    }

    public static Pair<Date, Date> getThisWeekPeriod() {
        Calendar cal = Calendar.getInstance();
        int firstDayOfWeek = cal.getFirstDayOfWeek();
        while (cal.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
            cal.add(Calendar.DATE, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        return new Pair<>(cal.getTime(), new Date());
    }

    public static Pair<Date, Date> getLastWeekPeriod() {
        Calendar cal = Calendar.getInstance();
        int firstDayOfWeek = cal.getFirstDayOfWeek();
        while (cal.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
            cal.add(Calendar.DATE, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);
        cal.add(Calendar.MILLISECOND, -1);
        Date end = cal.getTime();

        while (cal.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
            cal.add(Calendar.DATE, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        return new Pair<>(cal.getTime(), end);
    }

    public static Pair<Date, Date> getTodayPeriod() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        return new Pair<>(cal.getTime(), new Date());
    }

    public static Pair<Date, Date> getThisMonthPeriod() {
        Calendar cal = Calendar.getInstance();
        while (cal.get(Calendar.DATE) != 1) {
            cal.add(Calendar.DATE, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        return new Pair<>(cal.getTime(), new Date());
    }

    public static Pair<Date, Date> getLastMonthPeriod() {
        Calendar cal = Calendar.getInstance();
        while (cal.get(Calendar.DATE) != 1) {
            cal.add(Calendar.DATE, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);
        cal.add(Calendar.MILLISECOND, -1);
        Date end = cal.getTime();

        while (cal.get(Calendar.DATE) != 1) {
            cal.add(Calendar.DATE, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        return new Pair<>(cal.getTime(), end);
    }

    public static Pair<Date, Date> getThisYearPeriod() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        while (cal.get(Calendar.DATE) != 1) {
            cal.add(Calendar.DATE, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        return new Pair<>(cal.getTime(), new Date());
    }

    public static Pair<Date, Date> getLast30DaysPeriod() {
        long from = System.currentTimeMillis() - org.apache.commons.lang3.time.DateUtils.MILLIS_PER_DAY * 30;
        return new Pair<>(new Date(from), new Date());
    }

    public String toSerializeString() {
        return String.valueOf(type) + ":" + String.valueOf(startFrom);
    }

    public Pair<Date, Date> getCurrent() {
        return getPeriodWalker().getCurrent();
    }

    public Pair<Date, Date> getFullCurrent() {
        return getPeriodWalker().getFullCurrent();
    }

    public Pair<Date, Date> getPrevious(Pair<Date, Date> datePair) {
        return getPrevious(datePair.first, datePair.second);
    }

    public Pair<Date, Date> getPrevious(Date first, Date second) {
        return getPeriodWalker().getPrevious(first, second);
    }

    public Pair<Date, Date> getNext(Pair<Date, Date> datePair) {
        return getPeriodWalker().getNext(datePair);
    }

    private IPeriodWalker getPeriodWalker() {
        if (periodWalker == null || !periodWalker.same(type, startFrom)) {
            periodWalker = createPeriodWalker(type, startFrom);
        }
        return periodWalker;
    }

    private static IPeriodWalker createPeriodWalker(int type, int startFrom) {
        switch (type) {
            case MONTH_TYPE:
                return new MonthPeriodWalker(startFrom);
            case WEEK_TYPE:
                return new WeekPeriodWalker(startFrom);
            default:
                return null;
        }
    }
}
