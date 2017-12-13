package com.cactusteam.money.data.prediction;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author vpotapenko
 */
public class BudgetPrediction {

    public static final int UNDEFINED_STATE = 0;
    public static final int EARLY_FINISH_STATE = 1;
    public static final int NORMAL_STATE = 2;

    private int state = UNDEFINED_STATE;

    private Date willBeFinishedAt;
    private double speed;
    private double rest;

    public int getState() {
        return state;
    }

    public Date getWillBeFinishedAt() {
        return willBeFinishedAt;
    }

    public double getSpeed() {
        return speed;
    }

    public double getRest() {
        return rest;
    }

    public void calculate(Date startDate, Date finishDate, double amount, double limit) {
        state = UNDEFINED_STATE;

        rest = limit - amount;

        long now = System.currentTimeMillis();
        long start = startDate.getTime();

        if (start > now) {
            state = UNDEFINED_STATE;
            return;
        }

        int days = (int) TimeUnit.MILLISECONDS.toDays(now - start);
        if (days <= 0) {
            state = UNDEFINED_STATE;
            return;
        }

        speed = amount / ((double) days);
        if (speed <= 0) {
            state = UNDEFINED_STATE;
            return;
        }

        int restDays = (int) (Math.max(0, rest) / speed);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, restDays);

        willBeFinishedAt = calendar.getTime();

        if (rest > 0 && willBeFinishedAt.getTime() < finishDate.getTime()) {
            state = EARLY_FINISH_STATE;
        } else {
            state = NORMAL_STATE;
        }
    }
}
