package com.cactusteam.money.sync.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author vpotapenko
 */
public abstract class SyncObject {

    @JsonIgnore
    public long localId;
    @JsonIgnore
    public boolean removed;
    @JsonIgnore
    public Long preparedGlobalId;

    public long globalId;
}
