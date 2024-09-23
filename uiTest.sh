#!/bin/bash
#./gradlew :clean runIdeForUiTest&
#sleep 60
#./gradlew firstTimeUiTest
./gradlew buildPlugin
./gradlew uiTest
