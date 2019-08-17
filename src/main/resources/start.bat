
@set wintitle=SiChuanRainPrewarning

title %wintitle%

@set JAVA_HOME=D:\dev\sdk\jdk1.8.0_40x64
@set JRE_HOME=%JAVA_HOME%\jre
@echo JAVA env: JRE_HOME=%JRE_HOME%
@echo JAVA env: JAVA_HOME=%JAVA_HOME%
@echo Finding Jar..
@for /f "delims=" %%i in ('dir /b  /s "rain*.jar"') do @set JARPATH=%%i

@echo Start jar is %JARPATH%

@echo off
if exist %JAVA_HOME% (


if exist  %JARPATH% (
@echo Starting Service...

@call %JAVA_HOME%\bin\java -jar %JARPATH% -Dspring.config.location=application.properties -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=128m -Xms1024m -Xmx1024m -Xmn256m -Xss256k -XX:SurvivorRatio=8 -XX:+UseConcMarkSweepGC
）
else (
@echo 1111111
  @echo Check JStart jar   Path
)



）
else (
  @echo Check JAVA en  Path
)



PAUSE
