#!/bin/bash
./gradlew :clean :test

FILE=./build/reports/tests/SUCCESS.txt

if test -f "$FILE"; then
    ./gradlew runIdeForUiTest&
    sleep 30
    ./gradlew smokeUiTest
fi