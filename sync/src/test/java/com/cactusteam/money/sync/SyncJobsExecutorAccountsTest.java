package com.cactusteam.money.sync;

import com.cactusteam.money.sync.stubs.StubChangesStorage;
import com.cactusteam.money.sync.stubs.StubLogger;
import com.cactusteam.money.sync.stubs.StubProxyDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author vpotapenko
 */
public class SyncJobsExecutorAccountsTest {

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
    public void shouldUploadAccounts() throws Exception {
        com.cactusteam.money.sync.model.SyncAccount account = new com.cactusteam.money.sync.model.SyncAccount();
        account.globalId = -1;
        account.localId = 1;
        account.name = "My First Cash Account";
        account.currencyCode = "USD";
        account.type = 0;
        account.color = "someColorHere";
        proxyDatabase.dirtyAccounts.add(account);

        account = new com.cactusteam.money.sync.model.SyncAccount();
        account.globalId = -1;
        account.localId = 2;
        account.name = "My Second Cash Account";
        account.currencyCode = "EUR";
        account.type = 3;
        account.color = "someAnotherColorHere";
        proxyDatabase.dirtyAccounts.add(account);

        syncJobsExecutor.uploadAccounts();
        String actual = logStorage.getContent("accounts.ma").replaceAll("\r\n", "\n");
        String expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 0,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My First Cash Account\",\n" +
                "        \"currencyCode\" : \"USD\",\n" +
                "        \"color\" : \"someColorHere\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 0,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"type\" : 3,\n" +
                "        \"name\" : \"My Second Cash Account\",\n" +
                "        \"currencyCode\" : \"EUR\",\n" +
                "        \"color\" : \"someAnotherColorHere\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));

        proxyDatabase.dirtyAccounts.clear();
        account = new com.cactusteam.money.sync.model.SyncAccount();
        account.globalId = 1;
        account.localId = 1;
        account.name = "My First Cash Account";
        account.currencyCode = "USD";
        account.type = 0;
        account.color = "someColorHere2";
        proxyDatabase.dirtyAccounts.add(account);

        syncJobsExecutor.uploadAccounts();
        actual = logStorage.getContent("accounts.ma").replaceAll("\r\n", "\n");
        expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 0,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My First Cash Account\",\n" +
                "        \"currencyCode\" : \"USD\",\n" +
                "        \"color\" : \"someColorHere\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 0,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"type\" : 3,\n" +
                "        \"name\" : \"My Second Cash Account\",\n" +
                "        \"currencyCode\" : \"EUR\",\n" +
                "        \"color\" : \"someAnotherColorHere\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 0,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My First Cash Account\",\n" +
                "        \"currencyCode\" : \"USD\",\n" +
                "        \"color\" : \"someColorHere2\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));

        proxyDatabase.dirtyAccounts.clear();
        account = new com.cactusteam.money.sync.model.SyncAccount();
        account.globalId = 1;
        account.removed = true;
        proxyDatabase.dirtyAccounts.add(account);

        syncJobsExecutor.uploadAccounts();
        actual = logStorage.getContent("accounts.ma").replaceAll("\r\n", "\n");
        expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 0,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My First Cash Account\",\n" +
                "        \"currencyCode\" : \"USD\",\n" +
                "        \"color\" : \"someColorHere\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 0,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"type\" : 3,\n" +
                "        \"name\" : \"My Second Cash Account\",\n" +
                "        \"currencyCode\" : \"EUR\",\n" +
                "        \"color\" : \"someAnotherColorHere\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 0,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My First Cash Account\",\n" +
                "        \"currencyCode\" : \"USD\",\n" +
                "        \"color\" : \"someColorHere2\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 4,\n" +
                "    \"action\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 0,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));

        assertThat(proxyDatabase.committedItems.size(), equalTo(1));
        assertThat(proxyDatabase.committedItems.get(0).action, equalTo(SyncConstants.DELETE_ACTION));
        assertThat(proxyDatabase.committedItems.get(0).objectWrapper.type, equalTo(SyncConstants.ACCOUNT_TYPE));
        assertThat(proxyDatabase.committedItems.get(0).objectWrapper.obj, is((Object) account));

    }

    @Test
    public void shouldDownloadAccounts() throws Exception {
        String content = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 0,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My First Cash Account\",\n" +
                "        \"currencyCode\" : \"USD\",\n" +
                "        \"color\" : \"someColorHere\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 0,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My Second Cash Account\",\n" +
                "        \"currencyCode\" : \"EUR\",\n" +
                "        \"color\" : \"someAnotherColorHere\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 0,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My First Cash Account\",\n" +
                "        \"currencyCode\" : \"USD\",\n" +
                "        \"color\" : \"someColorHere2\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 4,\n" +
                "    \"action\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 0,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : null,\n" +
                "        \"currencyCode\" : null,\n" +
                "        \"color\" : null,\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        logStorage.setContent("accounts.ma", content);
        syncJobsExecutor.downloadAccounts();

        long id = 1;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            assertThat(c.id, equalTo(id));
            assertThat(c.type, equalTo(SyncConstants.ACCOUNT_TYPE));
            assertThat(c.obj, instanceOf(com.cactusteam.money.sync.model.SyncAccount.class));


            id++;
        }
    }

    @Test
    public void shouldRotateLogIfBigCommandsNumber() throws Exception {
        for (int i = 0; i < 3000; i++) {
            com.cactusteam.money.sync.model.SyncAccount account = new com.cactusteam.money.sync.model.SyncAccount();
            account.globalId = -1;
            account.localId = i;
            account.name = "Account" + i;
            account.currencyCode = "USD";
            account.type = 0;
            account.color = "color" + i;
            proxyDatabase.dirtyAccounts.add(account);
        }

        syncJobsExecutor.uploadAccounts();
        syncJobsExecutor.setDeviceId("some-another-id");
        syncJobsExecutor.downloadAccounts();

        int i = 0;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            com.cactusteam.money.sync.model.SyncAccount account = (com.cactusteam.money.sync.model.SyncAccount) c.obj;

            assertThat(c.type, equalTo(SyncConstants.ACCOUNT_TYPE));
            assertThat(c.action, equalTo(SyncConstants.CREATE_ACTION));
            assertThat(account.name, equalTo("Account" + i));
            assertThat(account.color, equalTo("color" + i));
            assertThat(account.currencyCode, equalTo("USD"));

            i++;
        }
        assertThat(i, equalTo(3000));
    }

    @Test
    public void shouldMergeNewIfFromCurrentDevis() throws Exception {
        for (int i = 0; i < 10; i++) {
            com.cactusteam.money.sync.model.SyncAccount account = new com.cactusteam.money.sync.model.SyncAccount();
            account.globalId = -1;
            account.localId = i;
            account.name = "Account" + i;
            account.currencyCode = "USD";
            account.type = 0;
            account.color = "color" + i;
            proxyDatabase.dirtyAccounts.add(account);
        }

        syncJobsExecutor.uploadAccounts();
        syncJobsExecutor.downloadAccounts();

        int i = 0;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            com.cactusteam.money.sync.model.SyncAccount account = (com.cactusteam.money.sync.model.SyncAccount) c.obj;

            assertThat(c.type, equalTo(SyncConstants.ACCOUNT_TYPE));
            assertThat(c.action, equalTo(-1));
            assertThat(account.name, equalTo("Account" + i));
            assertThat(account.color, equalTo("color" + i));
            assertThat(account.currencyCode, equalTo("USD"));

            i++;
        }
        assertThat(i, equalTo(10));
    }
}