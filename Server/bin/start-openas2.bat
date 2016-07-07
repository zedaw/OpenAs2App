@echo off
rem Purpose:  runs the OpenAS2 application

rem Uncomment any of the following for enhanced debug
rem set EXTRA_PARMS=%EXTRA_PARMS% -Dmaillogger.debug.enabled=true
rem set EXTRA_PARMS=%EXTRA_PARMS% -DlogRxdMsgMimeBodyParts=true
rem set EXTRA_PARMS=%EXTRA_PARMS% -DlogRxdMdnMimeBodyParts=true

rem Setup the Java Virtual Machine

    if not exist "%JAVA_HOME%" (
        call :warn JAVA_HOME is not valid: "%JAVA_HOME%"
        goto END
    )
    set JAVA=%JAVA_HOME%\bin\java


rem    
rem remove -Dorg.apache.commons.logging.Log=org.openas2.logging.Log if using another logging package
rem
@echo on
"%JAVA%" %EXTRA_PARMS% -Xms32m -Xmx384m  -Dorg.apache.commons.logging.Log=org.openas2.logging.Log -DCmdProcessorSocketCipher=SSL_DH_anon_WITH_RC4_128_MD5  -cp "..\lib\*" org.openas2.app.OpenAS2Server ..\config\config.xml

:warn
:END