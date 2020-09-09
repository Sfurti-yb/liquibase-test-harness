package liquibase.harness.util

import liquibase.CatalogAndSchema
import liquibase.command.core.SnapshotCommand
import liquibase.database.Database
import liquibase.util.StringUtil
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.JSONCompareResult
import org.skyscreamer.jsonassert.comparator.DefaultComparator
import org.skyscreamer.jsonassert.comparator.JSONCompareUtil

import static org.skyscreamer.jsonassert.comparator.JSONCompareUtil.getKeys
import static org.skyscreamer.jsonassert.comparator.JSONCompareUtil.qualify

class SnapshotHelpers {

    static String getJsonSnapshot(Database database, List<CatalogAndSchema> schemaList) {
        SnapshotCommand snapshotCommand = new SnapshotCommand()
        snapshotCommand.setDatabase(database)
        snapshotCommand.setSerializerFormat("json")
        if (!schemaList.isEmpty()) {
            snapshotCommand.setSchemas(schemaList.toArray(new CatalogAndSchema[schemaList.size()]))
        }

        SnapshotCommand.SnapshotCommandResult result = snapshotCommand.execute()
        return result.print()
    }


    static class GeneralSnapshotComparator extends DefaultComparator {
        GeneralSnapshotComparator() {
            super(JSONCompareMode.LENIENT)
        }

        @Override
        void compareJSONArray(String prefix, JSONArray exp, JSONArray act, JSONCompareResult result) throws JSONException {
            if (exp.length() != 0) {
                if (JSONCompareUtil.allSimpleValues(exp)) {
                    this.compareJSONArrayOfSimpleValues(prefix, exp, act, result)
                } else if (JSONCompareUtil.allJSONObjects(exp)) {
                    this.compareJSONArrayOfJsonObjects(prefix, exp, act, result)
                } else {
                    this.recursivelyCompareJSONArray(prefix, exp, act, result)
                }
            }
        }

        @Override
        void compareValues(String prefix, Object expectedValue, Object actualValue, JSONCompareResult result) throws JSONException {
            if (expectedValue instanceof String && actualValue instanceof String) {
                if (actualValue.matches(expectedValue)) {
                    result.passed()
                } else if (!StringUtil.equalsIgnoreCaseAndEmpty(expectedValue, actualValue)) {
                    result.fail(prefix, expectedValue, actualValue)
                }
            } else {
                super.compareValues(prefix, expectedValue, actualValue, result)
            }
        }

        @Override
        protected void checkJsonObjectKeysExpectedInActual(String prefix, JSONObject expected, JSONObject actual, JSONCompareResult result) throws JSONException {
            Set<String> expectedKeys = getKeys(expected)
            if (expected.has("_noMatch")) {
                expectedKeys.remove("_noMatch")
                for (String key : expectedKeys) {
                    if (actual.has(key)) {
                        result.fail(prefix, expected, actual)
                    } else {
                        result.passed()
                        return
                    }
                }
            }
            for (String key : expectedKeys) {
                Object expectedValue = expected.get(key)
                if (actual.has(key)) {
                    Object actualValue = actual.get(key)
                    compareValues(qualify(prefix, key), expectedValue, actualValue, result)
                } else {
                    result.missing(prefix, key)
                }
            }
        }
    }

}