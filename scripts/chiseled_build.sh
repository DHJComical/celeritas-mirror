#!/bin/bash
# ./gradlew chiseledBuild for weak machines that can't do it all at once
set -e

for version in $(cat settings.gradle  | grep "vers(" | sed 's/\w*vers("//' | sed 's/",.*//' | xargs); do

echo "Building ${version}"

./gradlew --no-daemon :"Set active version to ${version}"
./gradlew --no-daemon packageActive
done
