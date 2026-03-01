#!/bin/bash
set -e

./gradlew clean build -x test

docker compose up --build