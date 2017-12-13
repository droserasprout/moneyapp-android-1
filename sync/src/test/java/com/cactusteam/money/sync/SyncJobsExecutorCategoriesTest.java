package com.cactusteam.money.sync;

import com.cactusteam.money.sync.stubs.StubLogger;
import com.cactusteam.money.sync.stubs.StubProxyDatabase;
import com.cactusteam.money.sync.stubs.StubChangesStorage;

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
public class SyncJobsExecutorCategoriesTest {

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
    public void shouldUploadCategories() throws Exception {
        com.cactusteam.money.sync.model.SyncCategory category = new com.cactusteam.money.sync.model.SyncCategory();
        category.globalId = -1;
        category.localId = 1;
        category.name = "My Category";
        category.icon = "icon1";
        category.type = 0;
        proxyDatabase.dirtyCategories.add(category);

        category = new com.cactusteam.money.sync.model.SyncCategory();
        category.globalId = -1;
        category.localId = 2;
        category.name = "My Category 2";
        category.icon = null;
        category.deleted = true;
        category.type = 1;
        proxyDatabase.dirtyCategories.add(category);

        syncJobsExecutor.uploadCategories();
        String actual = logStorage.getContent("categories.ma").replaceAll("\r\n", "\n");
        String expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 1,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My Category\",\n" +
                "        \"icon\" : \"icon1\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 1,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"type\" : 1,\n" +
                "        \"name\" : \"My Category 2\",\n" +
                "        \"deleted\" : true\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));

        proxyDatabase.dirtyCategories.clear();
        category = new com.cactusteam.money.sync.model.SyncCategory();
        category.globalId = 1;
        category.localId = 1;
        category.name = "My Category";
        category.icon = "icon2";
        category.type = 0;
        proxyDatabase.dirtyCategories.add(category);

        syncJobsExecutor.uploadCategories();
        actual = logStorage.getContent("categories.ma").replaceAll("\r\n", "\n");
        expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 1,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My Category\",\n" +
                "        \"icon\" : \"icon1\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 1,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"type\" : 1,\n" +
                "        \"name\" : \"My Category 2\",\n" +
                "        \"deleted\" : true\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 1,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My Category\",\n" +
                "        \"icon\" : \"icon2\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));

        proxyDatabase.dirtyCategories.clear();
        category = new com.cactusteam.money.sync.model.SyncCategory();
        category.globalId = 1;
        category.localId = 1;
        category.removed = true;
        proxyDatabase.dirtyCategories.add(category);

        syncJobsExecutor.uploadCategories();
        actual = logStorage.getContent("categories.ma").replaceAll("\r\n", "\n");
        expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 1,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My Category\",\n" +
                "        \"icon\" : \"icon1\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 1,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"type\" : 1,\n" +
                "        \"name\" : \"My Category 2\",\n" +
                "        \"deleted\" : true\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 1,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My Category\",\n" +
                "        \"icon\" : \"icon2\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 4,\n" +
                "    \"action\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 1,\n" +
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
        assertThat(proxyDatabase.committedItems.get(0).objectWrapper.type, equalTo(SyncConstants.CATEGORY_TYPE));
        assertThat(proxyDatabase.committedItems.get(0).objectWrapper.obj, is((Object) category));
    }

    @Test
    public void shouldDownloadCategories() throws Exception {
        String content = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 1,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My Category\",\n" +
                "        \"icon\" : \"icon1\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 2,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 1,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"type\" : 1,\n" +
                "        \"name\" : \"My Category 2\",\n" +
                "        \"icon\" : \"icon1\",\n" +
                "        \"deleted\" : true\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 1,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : \"My Category\",\n" +
                "        \"icon\" : \"icon2\",\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 4,\n" +
                "    \"action\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 1,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"type\" : 0,\n" +
                "        \"name\" : null,\n" +
                "        \"icon\" : null,\n" +
                "        \"deleted\" : false\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";

        logStorage.setContent("categories.ma", content);
        syncJobsExecutor.downloadCategories();

        long id = 1;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            assertThat(c.id, equalTo(id));
            assertThat(c.type, equalTo(SyncConstants.CATEGORY_TYPE));
            assertThat(c.obj, instanceOf(com.cactusteam.money.sync.model.SyncCategory.class));


            id++;
        }
    }

    @Test
    public void shouldRotateLogIfBigCommandsNumber() throws Exception {
        for (int i = 0; i < 3000; i++) {
            com.cactusteam.money.sync.model.SyncCategory category = new com.cactusteam.money.sync.model.SyncCategory();
            category.globalId = i;
            category.localId = i;
            category.name = "My Category" + i;
            category.icon = "icon1";
            category.type = 0;
            proxyDatabase.dirtyCategories.add(category);
        }

        syncJobsExecutor.uploadCategories();
        syncJobsExecutor.setDeviceId("some-another-id");
        syncJobsExecutor.downloadCategories();

        int i = 0;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            com.cactusteam.money.sync.model.SyncCategory category = (com.cactusteam.money.sync.model.SyncCategory) c.obj;

            assertThat(c.type, equalTo(SyncConstants.CATEGORY_TYPE));
            assertThat(category.name, equalTo("My Category" + i));
            assertThat(category.globalId, equalTo((long) i));
            assertThat(category.icon, equalTo("icon1"));

            i++;
        }
        assertThat(i, equalTo(3000));
    }
}
