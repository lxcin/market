@ECHO OFF
SETLOCAL

SET MAVEN_PROJECTBASEDIR=%CD%
IF NOT "%MAVEN_PROJECTBASEDIR%"=="" SET MAVEN_PROJECTBASEDIR=%CD%
SET MAVEN_WRAPPER_DIR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper

IF EXIST "%MAVEN_WRAPPER_DIR%\maven-wrapper.jar" (
    SET MAVEN_WRAPPER_JAR="%MAVEN_WRAPPER_DIR%\maven-wrapper.jar"
) ELSE (
    ECHO Could not find maven-wrapper.jar
    EXIT /B 1
)

SET MAVEN_OPTS=-Xmx1024m -XX:MaxMetaspaceSize=256m

java %MAVEN_OPTS% -classpath %MAVEN_WRAPPER_JAR% org.apache.maven.wrapper.MavenWrapperMain %*

ENDLOCAL
