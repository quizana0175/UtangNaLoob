param([Parameter(ValueFromRemainingArguments = $true)]$MavenArgs)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-19"
$mavenHome = "C:\Program Files\NetBeans-12.6\netbeans\java\maven"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

& "$env:JAVA_HOME\bin\java.exe" `
    "-Dclassworlds.conf=$mavenHome\bin\m2.conf" `
    "-Dmaven.home=$mavenHome" `
    "-Dmaven.multiModuleProjectDirectory=$scriptDir" `
    -classpath "$mavenHome\boot\plexus-classworlds-2.6.0.jar" `
    org.codehaus.plexus.classworlds.launcher.Launcher `
    -f "$scriptDir\pom.xml" `
    @MavenArgs
