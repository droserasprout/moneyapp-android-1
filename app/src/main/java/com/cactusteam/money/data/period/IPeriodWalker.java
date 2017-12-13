package com.cactusteam.money.data.period;

import android.util.Pair;

import java.util.Date;

/**
 * @author vpotapenko
 */
public interface IPeriodWalker {

    Pair<Date, Date> getCurrent();

    Pair<Date, Date> getPrevious(Date first, Date second);

    Pair<Date, Date> getNext(Pair<Date, Date> datePair);

    boolean same(int type, int startFrom);

    Pair<Date,Date> getFullCurrent();
}
