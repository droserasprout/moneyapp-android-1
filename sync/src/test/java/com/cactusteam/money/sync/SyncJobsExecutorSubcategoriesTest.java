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
public class SyncJobsExecutorSubcategoriesTest {

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
    public void shouldUploadSubcategories() throws Exception {
        com.cactusteam.money.sync.model.SyncSubcategory subcategory = new com.cactusteam.money.sync.model.SyncSubcategory();
        subcategory.globalId = -1;
        subcategory.localId = 1;
        subcategory.name = "My Subcategory";
        subcategory.globalCategoryId = 1;
        proxyDatabase.dirtySubcategories.add(subcategory);

        subcategory = new com.cactusteam.money.sync.model.SyncSubcategory();
        subcategory.globalId = -1;
        subcategory.localId = 2;
        subcategory.name = "My Subcategory 2";
        subcategory.globalCategoryId = 1;
        subcategory.deleted = true;
        proxyDatabase.dirtySubcategories.add(subcategory);

        syncJobsExecutor.uploadSubcategories();
        String actual = logStorage.getContent("subcategories.ma").replaceAll("\r\n", "\n");
        String expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 2,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"name\" : \"My Subcategory\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 2,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"name\" : \"My Subcategory 2\",\n" +
                "        \"deleted\" : true\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));

        proxyDatabase.dirtySubcategories.clear();
        subcategory = new com.cactusteam.money.sync.model.SyncSubcategory();
        subcategory.globalId = 1;
        subcategory.localId = 1;
        subcategory.name = "My Another Name Subcategory";
        subcategory.globalCategoryId = 1;
        subcategory.deleted = false;
        proxyDatabase.dirtySubcategories.add(subcategory);

        syncJobsExecutor.uploadSubcategories();
        actual = logStorage.getContent("subcategories.ma").replaceAll("\r\n", "\n");
        expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 2,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"name\" : \"My Subcategory\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 2,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"name\" : \"My Subcategory 2\",\n" +
                "        \"deleted\" : true\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 2,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"name\" : \"My Another Name Subcategory\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));

        proxyDatabase.dirtySubcategories.clear();
        subcategory = new com.cactusteam.money.sync.model.SyncSubcategory();
        subcategory.globalId = 1;
        subcategory.removed = true;
        proxyDatabase.dirtySubcategories.add(subcategory);

        syncJobsExecutor.uploadSubcategories();
        actual = logStorage.getContent("subcategories.ma").replaceAll("\r\n", "\n");
        expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 2,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"name\" : \"My Subcategory\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 2,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"name\" : \"My Subcategory 2\",\n" +
                "        \"deleted\" : true\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 2,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"name\" : \"My Another Name Subcategory\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 4,\n" +
                "    \"action\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 2,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"globalCategoryId\" : 0,\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void shouldDownloadSubcategories() throws Exception {
        String content = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 2,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"name\" : \"My Subcategory\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 2,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"name\" : \"My Subcategory 2\",\n" +
                "        \"deleted\" : true\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 2,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"globalCategoryId\" : 1,\n" +
                "        \"name\" : \"My Another Name Subcategory\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 4,\n" +
                "    \"action\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 2,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"globalCategoryId\" : 0,\n" +
                "        \"name\" : null,\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        logStorage.setContent("subcategories.ma", content);
        syncJobsExecutor.downloadSubcategories();

        long id = 1;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            assertThat(c.id, equalTo(id));
            assertThat(c.type, equalTo(SyncConstants.SUBCATEGORY_TYPE));
            assertThat(c.obj, instanceOf(com.cactusteam.money.sync.model.SyncSubcategory.class));


            id++;
        }
    }

    @Test
    public void shouldRotateLogIfBigCommandsNumber() throws Exception {
        for (int i = 0; i < 3000; i++) {
            com.cactusteam.money.sync.model.SyncSubcategory subcategory = new com.cactusteam.money.sync.model.SyncSubcategory();
            subcategory.globalId = i;
            subcategory.localId = i;
            subcategory.name = "My Sub" + i;
            proxyDatabase.dirtySubcategories.add(subcategory);
        }

        syncJobsExecutor.uploadSubcategories();
        syncJobsExecutor.setDeviceId("some-another-id");
        syncJobsExecutor.downloadSubcategories();

        int i = 0;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            com.cactusteam.money.sync.model.SyncSubcategory subcategory = (com.cactusteam.money.sync.model.SyncSubcategory) c.obj;

            assertThat(c.type, equalTo(SyncConstants.SUBCATEGORY_TYPE));
            assertThat(subcategory.name, equalTo("My Sub" + i));
            assertThat(subcategory.globalId, equalTo((long) i));

            i++;
        }
        assertThat(i, equalTo(3000));
    }
}
