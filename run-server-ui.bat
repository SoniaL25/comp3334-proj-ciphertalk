@echo off
setlocal

start "CipherTalk Server" cmd /k "cd /d %~dp0server && mvnw.cmd spring-boot:run"

timeout /t 6 >nul
cd /d %~dp0
python -m client.client

endlocal