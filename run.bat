@echo off
setlocal
echo ====================================================
echo Starting CampusRide Desktop Application...
echo ====================================================

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if exist "target\campusride-1.0.0.jar" (
    java -jar target\campusride-1.0.0.jar
) else (
    echo Building project first with Maven...
    set "PATH=C:\Program Files\Apache\Maven\apache-maven-3.10.0\bin;%PATH%"
    call mvn clean package -DskipTests
    java -jar target\campusride-1.0.0.jar
)
endlocal
pause
