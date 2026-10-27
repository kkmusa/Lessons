#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/reminder-test
java -jar /workspace/tools/ecj.jar -encoding UTF-8 -source 8 -target 8 -d build/reminder-test android/src/ru/uroki/app/ReminderClock.java test/android/ReminderClockTest.java
java -cp build/reminder-test ru.uroki.app.ReminderClockTest
