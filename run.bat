@echo off
javac -cp ".;lib\sqlite-jdbc-3.43.2.2.jar;lib\slf4j-api-2.0.9.jar;lib\slf4j-simple-2.0.9.jar" -d . src\main\db\*.java src\main\model\*.java src\main\dao\*.java src\main\service\*.java src\main\app\*.java src\main\gui\*.java
java -cp ".;lib\sqlite-jdbc-3.43.2.2.jar;lib\slf4j-api-2.0.9.jar;lib\slf4j-simple-2.0.9.jar" main.app.Launcher