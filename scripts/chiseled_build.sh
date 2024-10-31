#!/bin/bash
# ./gradlew chiseledBuild for weak machines that can't do it all at once
set -e

for version in $(cat settings.gradle  | grep "vers(" | sed 's/\w*vers("//' | sed 's/",.*//' | xargs); do

echo "Building ${version}"

./gradlew "Set active project to ${version}"
./gradlew :${version}:packageActive
./gradlew --stop
done
