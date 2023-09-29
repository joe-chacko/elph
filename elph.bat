:: Copyright (c) 2023 IBM Corporation and others.
:: All rights reserved. This program and the accompanying materials
:: are made available under the terms of the Eclipse Public License 2.0
:: which accompanies this distribution, and is available at
:: http://www.eclipse.org/legal/epl-2.0/
::
:: SPDX-License-Identifier: EPL-2.0
::
:: Contributors:
::     IBM Corporation - initial API and implementation
@ECHO OFF

REM Prevent any variables from leaking into the calling environment
SETLOCAL

REM Find the ELPH install dir
SET "BASE_DIR=%~dp0"

REM Find the generated batch script
SET "SHELL_SCRIPT=%BASE_DIR%build\install\elph\bin\elph.bat"

REM Change directory (and drive if necessary) to the ELPH install dir
CD /D "%BASE_DIR%"

REM Check whether ELPH needs rebuilding
REM 1) If the batch script doesn't exist yet
IF NOT EXIST %SHELL_SCRIPT% goto :BUILD
REM 2) If the source files are newer
FOR /r src %%F IN (*) DO (
    REM Use the output of XCOPY /L /D /Y to check for newer files
    XCOPY /L /D /Y "%%F" "%SHELL_SCRIPT%" | FINDSTR /B /C:"1 " > NUL && GOTO :BUILD
)

GOTO :RUN
:BUILD
CALL .\gradlew.bat clean install

:RUN
REM Jansi doesn't work on Windows, so disable it 
SET JAVA_OPTS=-Dorg.fusesource.jansi.Ansi.disable=true
CALL %SHELL_SCRIPT% %*
