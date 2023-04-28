#!/bin/bash
./gradlew :clean runIdeForUiTest&
sleep 40
./gradlew firstTimeUiTest
#./gradlew uiTest