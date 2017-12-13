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
public class SyncJobsExecutorPatternsTest {

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
    public void shouldUploadPatterns() throws Exception {
        com.cactusteam.money.sync.model.SyncPattern pattern = new com.cactusteam.money.sync.model.SyncPattern();
        pattern.globalId = -1;
        pattern.localId = 1;
        pattern.name = "Name 1";
        pattern.type = 0;
        pattern.amount = 100;
        pattern.globalSourceAccountId = 1L;
        pattern.globalCategoryId = 1L;
        pattern.comment =  "Some comment";
        pattern.tags.add("Tag1");
        proxyDatabase.dirtyPatterns.add(pattern);

        pattern = new com.cactusteam.money.sync.model.SyncPattern();
        pattern.globalId = -1;
        pattern.localId = 2;
        pattern.name = "Name 2";
        pattern.type = 1;
        pattern.amount = 200;
        pattern.globalSourceAccountId = 2L;
        pattern.globalCategoryId = 3L;
        proxyDatabase.dirtyPatterns.add(pattern);

        pattern = new com.cactusteam.money.sync.model.SyncPattern();
        pattern.globalId = -1;
        pattern.localId = 3;
        pattern.name = "Name 3";
        pattern.type = 2;
        pattern.amount = 1000;
        pattern.destAmount = 1000D;
        pattern.globalSourceAccountId = 2L;
        pattern.globalDestAccountId = 1L;
        proxyDatabase.dirtyPatterns.add(pattern);

        syncJobsExecutor.uploadPatterns();
        String actual = logStorage.getContent("patterns.ma").replaceAll("\r\n", "\n");
        String expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"name\" : \"Name 1\",\n" +
                "        \"type\" : 0,\n" +
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
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"name\" : \"Name 2\",\n" +
                "        \"type\" : 1,\n" +
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
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 3,\n" +
                "        \"name\" : \"Name 3\",\n" +
                "        \"type\" : 2,\n" +
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

        proxyDatabase.dirtyPatterns.clear();
        pattern = new com.cactusteam.money.sync.model.SyncPattern();
        pattern.globalId = 3;
        pattern.localId = 3;
        pattern.name = "Name 4";
        pattern.type = 2;
        pattern.amount = 1500;
        pattern.destAmount = 1000D;
        pattern.globalSourceAccountId = 2L;
        pattern.globalDestAccountId = 1L;
        proxyDatabase.dirtyPatterns.add(pattern);

        syncJobsExecutor.uploadPatterns();
        actual = logStorage.getContent("patterns.ma").replaceAll("\r\n", "\n");
        expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"name\" : \"Name 1\",\n" +
                "        \"type\" : 0,\n" +
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
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"name\" : \"Name 2\",\n" +
                "        \"type\" : 1,\n" +
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
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 3,\n" +
                "        \"name\" : \"Name 3\",\n" +
                "        \"type\" : 2,\n" +
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
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 3,\n" +
                "        \"name\" : \"Name 4\",\n" +
                "        \"type\" : 2,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"amount\" : 1500.0,\n" +
                "        \"globalDestAccountId\" : 1,\n" +
                "        \"destAmount\" : 1000.0,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        assertThat(actual, equalTo(expected));

        proxyDatabase.dirtyPatterns.clear();
        pattern = new com.cactusteam.money.sync.model.SyncPattern();
        pattern.globalId = 3;
        pattern.removed = true;
        proxyDatabase.dirtyPatterns.add(pattern);

        syncJobsExecutor.uploadPatterns();
        actual = logStorage.getContent("patterns.ma").replaceAll("\r\n", "\n");
        expected = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"sourceDeviceId\" : \"some-device-id\",\n" +
                "    \"sourceId\" : 1,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"name\" : \"Name 1\",\n" +
                "        \"type\" : 0,\n" +
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
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"name\" : \"Name 2\",\n" +
                "        \"type\" : 1,\n" +
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
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 3,\n" +
                "        \"name\" : \"Name 3\",\n" +
                "        \"type\" : 2,\n" +
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
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 3,\n" +
                "        \"name\" : \"Name 4\",\n" +
                "        \"type\" : 2,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"amount\" : 1500.0,\n" +
                "        \"globalDestAccountId\" : 1,\n" +
                "        \"destAmount\" : 1000.0,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 5,\n" +
                "    \"action\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 3,\n" +
                "        \"type\" : 0,\n" +
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
    public void shouldDownloadPatterns() throws Exception {
        String content = "{\n" +
                "  \"version\" : 0,\n" +
                "  \"previousVersion\" : -1,\n" +
                "  \"items\" : [ {\n" +
                "    \"id\" : 1,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 1,\n" +
                "        \"name\" : \"Name 1\",\n" +
                "        \"type\" : 0,\n" +
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
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 2,\n" +
                "        \"name\" : \"Name 2\",\n" +
                "        \"type\" : 1,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"amount\" : 200.0,\n" +
                "        \"globalCategoryId\" : 3,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 3,\n" +
                "    \"action\" : 0,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 3,\n" +
                "        \"name\" : \"Name 3\",\n" +
                "        \"type\" : 2,\n" +
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
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 3,\n" +
                "        \"name\" : \"Name 4\",\n" +
                "        \"type\" : 2,\n" +
                "        \"globalSourceAccountId\" : 2,\n" +
                "        \"amount\" : 1500.0,\n" +
                "        \"globalDestAccountId\" : 1,\n" +
                "        \"destAmount\" : 1000.0,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"id\" : 5,\n" +
                "    \"action\" : 2,\n" +
                "    \"objectWrapper\" : {\n" +
                "      \"type\" : 5,\n" +
                "      \"obj\" : {\n" +
                "        \"globalId\" : 3,\n" +
                "        \"type\" : 0,\n" +
                "        \"globalSourceAccountId\" : 0,\n" +
                "        \"amount\" : 0.0,\n" +
                "        \"tags\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        logStorage.setContent("patterns.ma", content);
        syncJobsExecutor.downloadPatterns();

        long id = 1;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            assertThat(c.id, equalTo(id));
            assertThat(c.type, equalTo(SyncConstants.PATTERN_TYPE));
            assertThat(c.obj, instanceOf(com.cactusteam.money.sync.model.SyncPattern.class));

            id++;
        }
    }

    @Test
    public void shouldRotateLogIfBigCommandsNumber() throws Exception {
        for (int i = 0; i < 3000; i++) {
            com.cactusteam.money.sync.model.SyncPattern pattern = new com.cactusteam.money.sync.model.SyncPattern();
            pattern.globalId = i;
            pattern.localId = i;
            pattern.name = "Name" + i;
            pattern.type = 1;
            pattern.globalSourceAccountId = 1L;
            pattern.globalCategoryId = 2L;
            pattern.globalSubcategoryId = 3L;
            proxyDatabase.dirtyPatterns.add(pattern);
        }

        syncJobsExecutor.uploadPatterns();
        syncJobsExecutor.setDeviceId("some-another-id");
        syncJobsExecutor.syncPatterns();

        int i = 0;
        for (StubProxyDatabase.DbCommand c : proxyDatabase.oldCommands) {
            com.cactusteam.money.sync.model.SyncPattern pattern = (com.cactusteam.money.sync.model.SyncPattern) c.obj;

            assertThat(c.type, equalTo(SyncConstants.PATTERN_TYPE));
            assertThat(pattern.globalId, equalTo((long) i));
            assertThat(pattern.name, equalTo("Name" + i));
            assertThat(pattern.globalSubcategoryId, equalTo((long) 3));

            i++;
        }
        assertThat(i, equalTo(3000));
    }
}
