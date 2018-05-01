#!/usr/bin/env bash

if [ -n "$TRAVIS_TAG" ]
then
  ./gradlew publishPlugin
fi
