Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "  CAMPUSRIDE — Smart Student Ride-Pooling System   " -ForegroundColor Green
Write-Host "====================================================" -ForegroundColor Cyan

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;C:\Program Files\Apache\Maven\apache-maven-3.10.0\bin;$env:Path"

if (Test-Path "target\campusride-1.0.0.jar") {
    Write-Host "Launching CampusRide..." -ForegroundColor Yellow
    java -jar target\campusride-1.0.0.jar
} else {
    Write-Host "Building CampusRide executable JAR with Maven..." -ForegroundColor Yellow
    mvn clean package -DskipTests
    java -jar target\campusride-1.0.0.jar
}
