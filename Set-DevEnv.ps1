$env:HTTP_PROXY = 'http://127.0.0.1:7897'
$env:HTTPS_PROXY = 'http://127.0.0.1:7897'
$env:NO_PROXY = 'localhost,127.0.0.1,::1,*.local,10.*,172.16.*,192.168.*'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$javaBin = Join-Path $env:JAVA_HOME 'bin'
if (($env:PATH -split ';') -notcontains $javaBin) {
    $env:PATH = "$javaBin;$env:PATH"
}
