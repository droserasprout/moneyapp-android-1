package com.cactusteam.money.sync.changes;

import com.cactusteam.money.sync.model.SyncObject;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @author vpotapenko
 */
@JsonDeserialize(using = ObjectWrapperDeserializer.class)
public class ObjectWrapper {

    public int type;
    public SyncObject obj;

    public ObjectWrapper() {
    }

    public ObjectWrapper(int type, SyncObject obj) {
        this.type = type;
        this.obj = obj;
    }
}
