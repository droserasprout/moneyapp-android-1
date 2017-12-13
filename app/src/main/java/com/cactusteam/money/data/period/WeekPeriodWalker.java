package com.cactusteam.money.data.period;

import android.util.Pair;

import java.util.Calendar;
import java.util.Date;

/**
 * @author vpotapenko
 */
public class WeekPeriodWalker implements IPeriodWalker {

    private final int startFrom;

    public WeekPeriodWalker(int startFrom) {
        this.startFrom = startFrom;
    }

    @Override
    public Pair<Date, Date> getCurrent() {
        Calendar cal = Calendar.getInstance();
        while (cal.get(Calendar.DAY_OF_WEEK) != startFrom) {
            cal.add(Calendar.DATE, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        return new Pair<>(cal.getTime(), new Date());
    }

    @Override
    public Pair<Date, Date> getFullCurrent() {
        Calendar cal = Calendar.getInstance();
        while (cal.get(Calendar.DAY_OF_WEEK) != startFrom) {
            cal.add(Calendar.DATE, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);
        Date from = cal.getTime();

        cal.add(Calendar.DATE, 1);
        while (cal.get(Calendar.DAY_OF_WEEK) != startFrom) {
            cal.add(Calendar.DATE, 1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);
        cal.add(Calendar.MILLISECOND, -1);

        return new Pair<>(from, cal.getTime());
    }

    @Override
    public Pair<Date, Date> getPrevious(Date first, Date second) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(first);
        cal.add(Calendar.MILLISECOND, -1);
        Date to = cal.getTime();

        while (cal.get(Calendar.DAY_OF_WEEK) != startFrom) {
            cal.add(Calendar.DATE, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        return new Pair<>(cal.getTime(), to);
    }

    @Override
    public Pair<Date, Date> getNext(Pair<Date, Date> datePair) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(datePair.second);
        while (cal.get(Calendar.DAY_OF_WEEK) != startFrom) {
            cal.add(Calendar.DATE, 1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);
        Date from = cal.getTime();

        cal.add(Calendar.DATE, 1);
        while (cal.get(Calendar.DAY_OF_WEEK) != startFrom) {
            cal.add(Calendar.DATE, 1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);
        cal.add(Calendar.MILLISECOND, -1);

        return new Pair<>(from, cal.getTime());
    }

    @Override
    public boolean same(int type, int startFrom) {
        return type == Period.WEEK_TYPE && this.startFrom == startFrom;
    }
}
