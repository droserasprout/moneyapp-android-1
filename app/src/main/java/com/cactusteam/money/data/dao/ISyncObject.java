package com.cactusteam.money.data.dao;

/**
 * @author vpotapenko
 */
public interface ISyncObject {

    Long getId();

    Long getGlobalId();

    void setGlobalId(Long globalId);

    Boolean getSynced();

    void setSynced(Boolean synced);
}
