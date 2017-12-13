package com.cactusteam.money.sync.changes;

/**
 * @author vpotapenko
 */
public class ChangeItem {

    public long id;
    public int action;

    public String sourceDeviceId;
    public Long sourceId;

    public ObjectWrapper objectWrapper;

    public ChangeItem() {
    }

    public ChangeItem(long id, int action, ObjectWrapper objectWrapper) {
        this.id = id;
        this.action = action;
        this.objectWrapper = objectWrapper;
    }
}
