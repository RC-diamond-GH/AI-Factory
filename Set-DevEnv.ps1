$env:HTTP_PROXY = 'http://127.0.0.1:7897'
$env:HTTPS_PROXY = 'http://127.0.0.1:7897'
$env:NO_PROXY = 'localhost,127.0.0.1,::1,*.local,10.*,172.16.*,192.168.*'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:JAVA17_HOME = Join-Path $PSScriptRoot '.tools\jdk-17.0.20.1+1'
# Keep workstation-only proxy settings out of the portable Gradle project properties.
$proxyOptions = '-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'
if ($env:GRADLE_OPTS -notlike '*-Dhttps.proxyHost=*') {
    $env:GRADLE_OPTS = "$env:GRADLE_OPTS $proxyOptions".Trim()
}
$javaBin = Join-Path $env:JAVA_HOME 'bin'
if (($env:PATH -split ';') -notcontains $javaBin) {
    $env:PATH = "$javaBin;$env:PATH"
}
