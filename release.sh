#!/usr/bin/env bash

if [ -n "$TRAVIS_TAG" ]
then
  ./gradlew publishPlugin
else
  echo "Not running for tag as TRAVIS_TAG not set. Skipping ..."
fi
