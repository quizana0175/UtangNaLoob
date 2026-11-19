@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-19"
set "MAVEN_HOME=C:\Program Files\NetBeans-12.6\netbeans\java\maven"
"%JAVA_HOME%\bin\java.exe" "-Dclassworlds.conf=%MAVEN_HOME%\bin\m2.conf" "-Dmaven.home=%MAVEN_HOME%" "-Dmaven.multiModuleProjectDirectory=%~dp0." -classpath "%MAVEN_HOME%\boot\plexus-classworlds-2.6.0.jar" org.codehaus.plexus.classworlds.launcher.Launcher -f "%~dp0pom.xml" %*
