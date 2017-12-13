package com.cactusteam.money.sync.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author vpotapenko
 */
public class SyncPattern extends SyncObject {

    public String name;

    public int type;

    public long globalSourceAccountId;
    public String comment;

    public double amount;

    public Long globalCategoryId;
    public Long globalSubcategoryId;
    public Long globalDestAccountId;
    public Double destAmount;

    public List<String> tags = new ArrayList<>();
}
