package com.cactusteam.money.sync.changes;

import java.util.ArrayList;
import java.util.List;

/**
 * @author vpotapenko
 */
public class ChangesList {

    public int version;
    public int previousVersion = -1;

    public List<ChangeItem> items = new ArrayList<>();

    public long nextCommandId() {
        long maxId = 0;
        for (ChangeItem item : items) {
            maxId = Math.max(item.id, maxId);
        }
        return maxId + 1;
    }

    public long nextGlobalId() {
        long maxId = 0;
        for (ChangeItem item : items) {
            maxId = Math.max(item.objectWrapper.obj.globalId, maxId);
        }
        return maxId + 1;
    }
}
