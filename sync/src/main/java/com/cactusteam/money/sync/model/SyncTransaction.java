package com.cactusteam.money.sync.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author vpotapenko
 */
public class SyncTransaction extends SyncObject {

    public int type;

    public long date;
    public long globalSourceAccountId;
    public String comment;

    // ref should reference to global id
    public String ref;

    public int status;

    public double amount;

    public Long globalCategoryId;
    public Long globalSubcategoryId;
    public Long globalDestAccountId;
    public Double destAmount;

    public List<String> tags = new ArrayList<>();
}
