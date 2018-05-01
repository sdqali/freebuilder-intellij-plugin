#!/usr/bin/env bash

if [ -n "$TRAVIS_TAG" ]
then
  ./gradlew clean build publishPlugin
fi
