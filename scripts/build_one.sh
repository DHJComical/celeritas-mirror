#!/bin/bash
./gradlew :modern:"Set active project to $1"
./gradlew :modern:$1:packageJar
./gradlew :modern:"Reset active project"
