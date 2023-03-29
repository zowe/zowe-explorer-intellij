To run UI tests:
* change values for ZOS_USERID, ZOS_PWD, CONNECTION_URL in src/uiTest/kotlin/auxiliary/utils.kt
* run the script uiTest.sh
* once IdeForUiTests started make it as main window on the screen and do not touch mouse anymore

UI tests results: build/reports/tests/uiTest/index.html

To run Smoke test:
* change values for ZOS_USERID, ZOS_PWD, CONNECTION_URL in src/uiTest/kotlin/auxiliary/utils.kt
* run the script smokeTest.sh
* if unit tests fail, smoke ui test will be skipped. When unit tests are successful, IdeForUiTests will be run 
* once IdeForUiTests started make it as main window on the screen and do not touch mouse anymore

Smoke test results: build/reports/tests/test/index.html with report for unit tests, 
build/reports/tests/SUCCESS(FAILURE).txt with quick summary for unit test run (file name depends on test run result),
build/reports/tests/smokeUiTest/index.html with report for smoke UI test
