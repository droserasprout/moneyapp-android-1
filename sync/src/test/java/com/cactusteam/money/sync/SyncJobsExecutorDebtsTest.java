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
public class SyncJobsExecutorDebtsTest {

    private SyncJobsExecutor syncJobsExecutor;
    private StubProxyDatabase proxyDatabase;
    private StubChangesStorage logStorage;
    private StubLogger logger;

    @Before
    public void setUp() throws Exception {
        proxyDatabase = new StubProxyDatabase();
        logStorage = new StubChangesStorage();
        logger = new StubLogger();
        syncJobsExecutor = new SyncJobsExecutor(proxyDatabase, logStorage, logger, "some-device-id");
    }

    @After
    public void tearDown() throws Exception {
        logStorage.clearTestFolder();
    }

    @Test
    public void shouldUploadDebts() throws Exception {
        com.cactusteam.money.sync.model.SyncDebt debt = new com.cactusteam.money.sync.model.SyncDebt();
        debt.globalId = -1;
        debt.localId = 1;
        debt.name = "Debt name";
        debt.phone = "some phone here";
        proxyDatabase.dirtyDebts.add(debt);

        debt = new com.cactusteam.money.sync.model.SyncDebt();
        debt.globalId = -1;
        debt.localId = 2;
        debt.name = "Debt name 2";
        proxyDatabase.dirtyDebts.add(debt);

        syncJobsExecutor.uploadDebts();
        String actual = logStorage.getContent("debts.ma").replaceAll("\r\n", "\n");
        String expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 3,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"name\" : \"Debt name\",\n" +
                "        \"phone\" : \"some phone here\",\n" +
                "        \"globalAccountId\" : 0\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 3,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"name\" : \"Debt name 2\",\n" +
                "        \"globalAccountId\" : 0\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));

        proxyDatabase.dirtyDebts.clear();
        debt = new com.cactusteam.money.sync.model.SyncDebt();
        debt.globalId = 2;
        debt.localId = 2;
        debt.name = "Debt name 2";
        debt.phone = "phone number";
        proxyDatabase.dirtyDebts.add(debt);

        syncJobsExecutor.uploadDebts();
        actual = logStorage.getContent("debts.ma").replaceAll("\r\n", "\n");
        expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 3,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"name\" : \"Debt name\",\n" +
                "        \"phone\" : \"some phone here\",\n" +
                "        \"globalAccountId\" : 0\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 3,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"name\" : \"Debt name 2\",\n" +
                "        \"globalAccountId\" : 0\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 3,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"name\" : \"Debt name 2\",\n" +
                "        \"phone\" : \"phone number\",\n" +
                "        \"globalAccountId\" : 0\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));

        proxyDatabase.dirtyDebts.clear();
        debt = new com.cactusteam.money.sync.model.SyncDebt();
        debt.globalId = 2;
        debt.removed = true;
        proxyDatabase.dirtyDebts.add(debt);

        syncJobsExecutor.uploadDebts();
        actual = logStorage.getContent("debts.ma").replaceAll("\r\n", "\n");
        expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 3,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"name\" : \"Debt name\",\n" +
                "        \"phone\" : \"some phone here\",\n" +
                "        \"globalAccountId\" : 0\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 3,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"name\" : \"Debt name 2\",\n" +
                "        \"globalAccountId\" : 0\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 3,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"name\" : \"Debt name 2\",\n" +
                "        \"phone\" : \"phone number\",\n" +
                "        \"globalAccountId\" : 0\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 4,\n" +
                "    \"action\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 3,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"globalAccountId\" : 0\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void shouldDownloadDebts() throws Exception {
        String content = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 3,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"name\" : \"Debt name\",\n" +
                "        \"phone\" : \"some phone here\",\n" +
                "        \"globalAccountId\" : 0\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 3,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"name\" : \"Debt name 2\",\n" +
                "        \"globalAccountId\" : 0\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 3,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"name\" : \"Debt name 2\",\n" +
                "        \"phone\" : \"phone number\",\n" +
                "        \"globalAccountId\" : 0\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 4,\n" +
                "    \"action\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 3,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"globalAccountId\" : 0\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        logStorage.setContent("debts.ma", content);
        syncJobsExecutor.downloadDebts();

        long id = 1;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            assertThat(c.id, equalTo(id));
            assertThat(c.type, equalTo(SyncConstants.DEBT_TYPE));
            assertThat(c.obj, instanceOf(com.cactusteam.money.sync.model.SyncDebt.class));


            id++;
        }
    }

    @Test
    public void shouldRotateLogIfBigCommandsNumber() throws Exception {
        for (int i = 0; i < 3000; i++) {
            com.cactusteam.money.sync.model.SyncDebt debt = new com.cactusteam.money.sync.model.SyncDebt();
            debt.globalId = i;
            debt.localId = i;
            debt.name = "My Debt" + i;
            proxyDatabase.dirtyDebts.add(debt);
        }

        syncJobsExecutor.uploadDebts();
        syncJobsExecutor.setDeviceId("some-another-id");
        syncJobsExecutor.syncDebts();

        int i = 0;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            com.cactusteam.money.sync.model.SyncDebt debt = (com.cactusteam.money.sync.model.SyncDebt) c.obj;

            assertThat(c.type, equalTo(SyncConstants.DEBT_TYPE));
            assertThat(debt.name, equalTo("My Debt" + i));
            assertThat(debt.globalId, equalTo((long) i));

            i++;
        }
        assertThat(i, equalTo(3000));
        String expectedLog = "Uploading debts\nDownloading debts\nUploading debts\n";
        assertThat(logger.sb.toString(), equalTo(expectedLog));
    }
}
