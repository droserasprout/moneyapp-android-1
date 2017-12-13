package com.cactusteam.money.sync;

import com.cactusteam.money.sync.model.SyncBudget;
import com.cactusteam.money.sync.stubs.StubChangesStorage;
import com.cactusteam.money.sync.stubs.StubLogger;
import com.cactusteam.money.sync.stubs.StubProxyDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * @author vpotapenko
 */
public class SyncJobsExecutorBudgetsTest {

    private SyncJobsExecutor syncJobsExecutor;
    private StubProxyDatabase proxyDatabase;
    private StubChangesStorage logStorage;

    @Before
    public void setUp() throws Exception {
        proxyDatabase = new StubProxyDatabase();
        logStorage = new StubChangesStorage();
        StubLogger logger = new StubLogger();
        syncJobsExecutor = new SyncJobsExecutor(proxyDatabase, logStorage, logger, "some-device-id");
    }

    @After
    public void tearDown() throws Exception {
        logStorage.clearTestFolder();
    }

    @Test
    public void shouldUploadBudgets() throws Exception {
        SyncBudget budget = new SyncBudget();
        budget.globalId = -1;
        budget.localId = 1;
        budget.start = 1001;
        budget.finish = 1020;
        budget.limit = 10000;
        budget.type = 1;
        budget.name = "Budget 1";
        budget.nextGlobalId = 1L;
        budget.dependencies.add(new SyncBudget.Dependency(2, "10101"));
        budget.dependencies.add(new SyncBudget.Dependency(1, "101"));
        proxyDatabase.dirtyBudgets.add(budget);

        budget = new SyncBudget();
        budget.globalId = -1;
        budget.localId = 2;
        budget.start = 100;
        budget.finish = 102;
        budget.limit = 12000;
        budget.type = 0;
        budget.name = "Budget 2";
        budget.dependencies.add(new SyncBudget.Dependency(0, "111"));
        proxyDatabase.dirtyBudgets.add(budget);

        syncJobsExecutor.uploadBudgets();
        String actual = logStorage.getContent("budgets.ma").replaceAll("\r\n", "\n");
        String expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 6,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"start\" : 1001,\n" +
                "        \"finish\" : 1020,\n" +
                "        \"limit\" : 10000.0,\n" +
                "        \"type\" : 1,\n" +
                "        \"name\" : \"Budget 1\",\n" +
                "        \"nextGlobalId\" : 1,\n" +
                "        \"dependencies\" : [ {\n" +
                "          \"type\" : 2,\n" +
                "          \"refGlobalId\" : \"10101\"\n" +
                "        }, {\n" +
                "          \"type\" : 1,\n" +
                "          \"refGlobalId\" : \"101\"\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 6,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"start\" : 100,\n" +
                "        \"finish\" : 102,\n" +
                "        \"limit\" : 12000.0,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"Budget 2\",\n" +
                "        \"dependencies\" : [ {\n" +
                "          \"type\" : 0,\n" +
                "          \"refGlobalId\" : \"111\"\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));

        proxyDatabase.dirtyBudgets.clear();
        budget = new SyncBudget();
        budget.globalId = 2;
        budget.localId = 2;
        budget.start = 100;
        budget.finish = 102;
        budget.limit = 20000;
        budget.type = 1;
        budget.name = "Budget 2";
        budget.dependencies.add(new SyncBudget.Dependency(0, "111"));
        proxyDatabase.dirtyBudgets.add(budget);

        syncJobsExecutor.uploadBudgets();
        actual = logStorage.getContent("budgets.ma").replaceAll("\r\n", "\n");
        expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 6,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"start\" : 1001,\n" +
                "        \"finish\" : 1020,\n" +
                "        \"limit\" : 10000.0,\n" +
                "        \"type\" : 1,\n" +
                "        \"name\" : \"Budget 1\",\n" +
                "        \"nextGlobalId\" : 1,\n" +
                "        \"dependencies\" : [ {\n" +
                "          \"type\" : 2,\n" +
                "          \"refGlobalId\" : \"10101\"\n" +
                "        }, {\n" +
                "          \"type\" : 1,\n" +
                "          \"refGlobalId\" : \"101\"\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 6,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"start\" : 100,\n" +
                "        \"finish\" : 102,\n" +
                "        \"limit\" : 12000.0,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"Budget 2\",\n" +
                "        \"dependencies\" : [ {\n" +
                "          \"type\" : 0,\n" +
                "          \"refGlobalId\" : \"111\"\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 6,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"start\" : 100,\n" +
                "        \"finish\" : 102,\n" +
                "        \"limit\" : 20000.0,\n" +
                "        \"type\" : 1,\n" +
                "        \"name\" : \"Budget 2\",\n" +
                "        \"dependencies\" : [ {\n" +
                "          \"type\" : 0,\n" +
                "          \"refGlobalId\" : \"111\"\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));

        proxyDatabase.dirtyBudgets.clear();
        budget = new SyncBudget();
        budget.globalId = 2;
        budget.removed = true;
        proxyDatabase.dirtyBudgets.add(budget);

        syncJobsExecutor.uploadBudgets();
        actual = logStorage.getContent("budgets.ma").replaceAll("\r\n", "\n");
        expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 6,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"start\" : 1001,\n" +
                "        \"finish\" : 1020,\n" +
                "        \"limit\" : 10000.0,\n" +
                "        \"type\" : 1,\n" +
                "        \"name\" : \"Budget 1\",\n" +
                "        \"nextGlobalId\" : 1,\n" +
                "        \"dependencies\" : [ {\n" +
                "          \"type\" : 2,\n" +
                "          \"refGlobalId\" : \"10101\"\n" +
                "        }, {\n" +
                "          \"type\" : 1,\n" +
                "          \"refGlobalId\" : \"101\"\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 6,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"start\" : 100,\n" +
                "        \"finish\" : 102,\n" +
                "        \"limit\" : 12000.0,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"Budget 2\",\n" +
                "        \"dependencies\" : [ {\n" +
                "          \"type\" : 0,\n" +
                "          \"refGlobalId\" : \"111\"\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 6,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"start\" : 100,\n" +
                "        \"finish\" : 102,\n" +
                "        \"limit\" : 20000.0,\n" +
                "        \"type\" : 1,\n" +
                "        \"name\" : \"Budget 2\",\n" +
                "        \"dependencies\" : [ {\n" +
                "          \"type\" : 0,\n" +
                "          \"refGlobalId\" : \"111\"\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 4,\n" +
                "    \"action\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 6,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"start\" : 0,\n" +
                "        \"finish\" : 0,\n" +
                "        \"limit\" : 0.0,\n" +
                "        \"type\" : 0,\n" +
                "        \"dependencies\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void shouldDownloadBudgets() throws Exception {
        String content = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 6,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"start\" : 1001,\n" +
                "        \"finish\" : 1020,\n" +
                "        \"limit\" : 10000.0,\n" +
                "        \"type\" : 1,\n" +
                "        \"name\" : \"Budget 1\",\n" +
                "        \"nextGlobalId\" : 1,\n" +
                "        \"dependencies\" : [ {\n" +
                "          \"type\" : 2,\n" +
                "          \"refGlobalId\" : \"10101\"\n" +
                "        }, {\n" +
                "          \"type\" : 1,\n" +
                "          \"refGlobalId\" : \"101\"\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 6,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"start\" : 100,\n" +
                "        \"finish\" : 102,\n" +
                "        \"limit\" : 12000.0,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"Budget 2\",\n" +
                "        \"dependencies\" : [ {\n" +
                "          \"type\" : 0,\n" +
                "          \"refGlobalId\" : \"111\"\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 6,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"start\" : 100,\n" +
                "        \"finish\" : 102,\n" +
                "        \"limit\" : 20000.0,\n" +
                "        \"type\" : 1,\n" +
                "        \"name\" : \"Budget 2\",\n" +
                "        \"dependencies\" : [ {\n" +
                "          \"type\" : 0,\n" +
                "          \"refGlobalId\" : \"111\"\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 4,\n" +
                "    \"action\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 6,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"start\" : 0,\n" +
                "        \"finish\" : 0,\n" +
                "        \"limit\" : 0.0,\n" +
                "        \"type\" : 0,\n" +
                "        \"dependencies\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";

        logStorage.setContent("budgets.ma", content);
        syncJobsExecutor.downloadBudgets();

        long id = 1;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            assertThat(c.id, equalTo(id));
            assertThat(c.type, equalTo(SyncConstants.BUDGET_TYPE));
            assertThat(c.obj, instanceOf(SyncBudget.class));

            id++;
        }
    }

    @Test
    public void shouldRotateLogIfBigCommandsNumber() throws Exception {
        for (int i = 0; i < 3000; i++) {
            SyncBudget budget = new SyncBudget();
            budget.globalId = i;
            budget.localId = i;
            budget.name = "My Budget" + i;
            proxyDatabase.dirtyBudgets.add(budget);
        }

        syncJobsExecutor.uploadBudgets();
        syncJobsExecutor.setDeviceId("some-another-id");
        syncJobsExecutor.syncBudgets();

        int i = 0;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            SyncBudget budget = (SyncBudget) c.obj;

            assertThat(c.type, equalTo(SyncConstants.BUDGET_TYPE));
            assertThat(budget.name, equalTo("My Budget" + i));
            assertThat(budget.globalId, equalTo((long) i));

            i++;
        }
        assertThat(i, equalTo(3000));
    }
}
