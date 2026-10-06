@echo off
rem Builds the runnable JAR and launches it.
call mvn -q clean package || exit /b 1
java -jar target\TwoZeroFourEightGame-1.0.0.jar
