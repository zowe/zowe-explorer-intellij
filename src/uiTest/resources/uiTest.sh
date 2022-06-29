#!/bin/bash
./gradlew runIdeForUiTest&
sleep 30
./gradlew firstTimeUiTest
./gradlew uiTest