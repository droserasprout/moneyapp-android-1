package com.cactusteam.money.sync.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author vpotapenko
 */
public class SyncBudget extends SyncObject {

    public long start;
    public long finish;
    public double limit;
    public int type;

    public String name;
    public Long nextGlobalId;

    public final List<Dependency> dependencies = new ArrayList<>();

    public static class Dependency {

        public int type;
        public String refGlobalId;

        public Dependency() {
        }

        public Dependency(int type, String refGlobalId) {
            this.type = type;
            this.refGlobalId = refGlobalId;
        }
    }
}
