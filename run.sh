#!/bin/sh
# Builds the runnable JAR and launches it.
set -e
mvn -q clean package
java -jar target/TwoZeroFourEightGame-1.0.0.jar
