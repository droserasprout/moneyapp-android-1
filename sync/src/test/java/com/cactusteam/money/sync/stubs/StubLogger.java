package com.cactusteam.money.sync.stubs;

import com.cactusteam.money.sync.ILogger;

/**
 * @author vpotapenko
 */
public class StubLogger implements ILogger {

    public StringBuilder sb = new StringBuilder();

    @Override
    public void print(String message) {
        sb.append(message).append('\n');
    }
}
