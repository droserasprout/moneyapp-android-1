package com.cactusteam.money.sync;

import com.cactusteam.money.sync.stubs.StubLogger;
import com.cactusteam.money.sync.stubs.StubProxyDatabase;
import com.cactusteam.money.sync.stubs.StubChangesStorage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * @author vpotapenko
 */
public class SyncJobsExecutorTransactionsTest {

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
    public void shouldUploadTransactions() throws Exception {
        com.cactusteam.money.sync.model.SyncTransaction transaction = new com.cactusteam.money.sync.model.SyncTransaction();
        transaction.globalId = -1;
        transaction.localId = 1;
        transaction.type = 0;
        transaction.amount = 100;
        transaction.globalSourceAccountId = 1L;
        transaction.globalCategoryId = 1L;
        transaction.comment =  "Some comment";
        transaction.date = 1000L;
        transaction.tags.add("Tag1");
        proxyDatabase.dirtyTransactions.add(transaction);

        transaction = new com.cactusteam.money.sync.model.SyncTransaction();
        transaction.globalId = -1;
        transaction.localId = 2;
        transaction.type = 1;
        transaction.amount = 200;
        transaction.globalSourceAccountId = 2L;
        transaction.globalCategoryId = 3L;
        transaction.date = 2000L;
        proxyDatabase.dirtyTransactions.add(transaction);

        transaction = new com.cactusteam.money.sync.model.SyncTransaction();
        transaction.globalId = -1;
        transaction.localId = 3;
        transaction.type = 2;
        transaction.amount = 1000;
        transaction.destAmount = 1000D;
        transaction.globalSourceAccountId = 2L;
        transaction.globalDestAccountId = 1L;
        transaction.date = 2000L;
        proxyDatabase.dirtyTransactions.add(transaction);

        syncJobsExecutor.uploadTransactions();
        String actual = logStorage.getContent("transactions.ma").replaceAll("\r\n", "\n");
        String expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"date\" : 1000,\n" +
                "        \"globalSourceAccountId\" : 1,\n" +
                "        \"comment\" : \"Some comment\",\n" +
                "        \"amount\" : 100.0,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"tags\" : [ \"Tag1\" ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"type\" : 1,\n" +
                "        \"date\" : 2000,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"amount\" : 200.0,\n" +
                "        \"globalCategoryId\" : 3,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 3,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 3,\n" +
                "        \"type\" : 2,\n" +
                "        \"date\" : 2000,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"amount\" : 1000.0,\n" +
                "        \"globalDestAccountId\" : 1,\n" +
                "        \"destAmount\" : 1000.0,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));

        proxyDatabase.dirtyTransactions.clear();
        transaction = new com.cactusteam.money.sync.model.SyncTransaction();
        transaction.globalId = 1;
        transaction.localId = 2;
        transaction.type = 1;
        transaction.amount = 300;
        transaction.globalSourceAccountId = 2L;
        transaction.globalCategoryId = 3L;
        transaction.date = 2000L;
        transaction.comment = "comment";
        transaction.ref = "ref here";
        proxyDatabase.dirtyTransactions.add(transaction);

        syncJobsExecutor.uploadTransactions();
        actual = logStorage.getContent("transactions.ma").replaceAll("\r\n", "\n");
        expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"date\" : 1000,\n" +
                "        \"globalSourceAccountId\" : 1,\n" +
                "        \"comment\" : \"Some comment\",\n" +
                "        \"amount\" : 100.0,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"tags\" : [ \"Tag1\" ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"type\" : 1,\n" +
                "        \"date\" : 2000,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"amount\" : 200.0,\n" +
                "        \"globalCategoryId\" : 3,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 3,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 3,\n" +
                "        \"type\" : 2,\n" +
                "        \"date\" : 2000,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"amount\" : 1000.0,\n" +
                "        \"globalDestAccountId\" : 1,\n" +
                "        \"destAmount\" : 1000.0,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 4,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 1,\n" +
                "        \"date\" : 2000,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"comment\" : \"comment\",\n" +
                "        \"ref\" : \"ref here\",\n" +
                "        \"amount\" : 300.0,\n" +
                "        \"globalCategoryId\" : 3,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));

        proxyDatabase.dirtyTransactions.clear();
        transaction = new com.cactusteam.money.sync.model.SyncTransaction();
        transaction.globalId = 1;
        transaction.removed = true;
        proxyDatabase.dirtyTransactions.add(transaction);

        syncJobsExecutor.uploadTransactions();
        actual = logStorage.getContent("transactions.ma").replaceAll("\r\n", "\n");
        expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"date\" : 1000,\n" +
                "        \"globalSourceAccountId\" : 1,\n" +
                "        \"comment\" : \"Some comment\",\n" +
                "        \"amount\" : 100.0,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"tags\" : [ \"Tag1\" ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"type\" : 1,\n" +
                "        \"date\" : 2000,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"amount\" : 200.0,\n" +
                "        \"globalCategoryId\" : 3,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 3,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 3,\n" +
                "        \"type\" : 2,\n" +
                "        \"date\" : 2000,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"amount\" : 1000.0,\n" +
                "        \"globalDestAccountId\" : 1,\n" +
                "        \"destAmount\" : 1000.0,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 4,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 1,\n" +
                "        \"date\" : 2000,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"comment\" : \"comment\",\n" +
                "        \"ref\" : \"ref here\",\n" +
                "        \"amount\" : 300.0,\n" +
                "        \"globalCategoryId\" : 3,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 5,\n" +
                "    \"action\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"date\" : 0,\n" +
                "        \"globalSourceAccountId\" : 0,\n" +
                "        \"amount\" : 0.0,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void shouldDownloadTransactions() throws Exception {
        String content = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"type\" : 4,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"date\" : 1000,\n" +
                "        \"globalSourceAccountId\" : 1,\n" +
                "        \"comment\" : \"Some comment\",\n" +
                "        \"amount\" : 100.0,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"tags\" : [ \"Tag1\" ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"type\" : 4,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"type\" : 1,\n" +
                "        \"date\" : 2000,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"amount\" : 200.0,\n" +
                "        \"globalCategoryId\" : 3,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"type\" : 4,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 3,\n" +
                "        \"type\" : 2,\n" +
                "        \"date\" : 2000,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"amount\" : 1000.0,\n" +
                "        \"globalDestAccountId\" : 1,\n" +
                "        \"destAmount\" : 1000.0,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 4,\n" +
                "    \"type\" : 4,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 1,\n" +
                "        \"date\" : 2000,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"comment\" : \"comment\",\n" +
                "        \"ref\" : \"ref here\",\n" +
                "        \"amount\" : 300.0,\n" +
                "        \"globalCategoryId\" : 3,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 5,\n" +
                "    \"type\" : 4,\n" +
                "    \"action\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 4,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"date\" : 0,\n" +
                "        \"globalSourceAccountId\" : 0,\n" +
                "        \"amount\" : 0.0,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        logStorage.setContent("transactions.ma", content);
        syncJobsExecutor.downloadTransactions();

        long id = 1;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            assertThat(c.id, equalTo(id));
            assertThat(c.type, equalTo(SyncConstants.TRANSACTION_TYPE));
            assertThat(c.obj, instanceOf(com.cactusteam.money.sync.model.SyncTransaction.class));

            id++;
        }
    }

    @Test
    public void shouldRotateLogIfBigCommandsNumber() throws Exception {
        for (int i = 0; i < 3000; i++) {
            com.cactusteam.money.sync.model.SyncTransaction transaction = new com.cactusteam.money.sync.model.SyncTransaction();
            transaction.globalId = i;
            transaction.localId = i;
            transaction.type = 1;
            transaction.globalSourceAccountId = 1L;
            transaction.globalCategoryId = 2L;
            transaction.globalSubcategoryId = 3L;
            proxyDatabase.dirtyTransactions.add(transaction);
        }

        syncJobsExecutor.uploadTransactions();
        syncJobsExecutor.setDeviceId("some-another-id");
        syncJobsExecutor.downloadTransactions();

        int i = 0;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            com.cactusteam.money.sync.model.SyncTransaction transaction = (com.cactusteam.money.sync.model.SyncTransaction) c.obj;

            assertThat(c.type, equalTo(SyncConstants.TRANSACTION_TYPE));
            assertThat(transaction.globalId, equalTo((long) i));
            assertThat(transaction.globalSubcategoryId, equalTo((long) 3));

            i++;
        }
        assertThat(i, equalTo(3000));
    }
}
