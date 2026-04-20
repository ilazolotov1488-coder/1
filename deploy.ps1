# deploy.ps1 — собрать, зашифровать, задеплоить
param([string]$version = "")

$KEY = "F7DB3118ADB25C0664BDE1C098AFAE55B0AFD3077FE9393A38CB56AD5C6A889C"
$IV  = "5BFC6C2D7AE699ADC0457EFE884136DE"
$JAR = "build\libs\space-betka-1.0-SNAPSHOT.jar"
$SERVER = "..\..\altmanager test\server"

Write-Host "[1] Building..." -ForegroundColor Cyan
./gradlew build -x test
if ($LASTEXITCODE -ne 0) { Write-Host "BUILD FAILED" -ForegroundColor Red; exit 1 }

Write-Host "[2] Encrypting..." -ForegroundColor Cyan
node "$SERVER\encrypt_fixed.js" $KEY $IV $JAR
if ($LASTEXITCODE -ne 0) { Write-Host "ENCRYPT FAILED" -ForegroundColor Red; exit 1 }

Write-Host "[3] Copying to server..." -ForegroundColor Cyan
Copy-Item "build\libs\mod.jar.enc" "$SERVER\mod.jar.enc" -Force
Copy-Item $JAR "C:\Users\ilzol\AppData\Roaming\.minecraft\mods\space-betka-1.0-SNAPSHOT.jar" -Force
Copy-Item $JAR "C:\Space Visuals\.minecraft\mods\space-betka-1.0-SNAPSHOT.jar" -Force
Copy-Item $JAR "C:\Space Visuals\.minecraft\mods\space-betka-1.0-SNAPSHOT.jar" -Force

if ($version -ne "") {
    Set-Content "C:\Space Visuals\jar_version.txt" $version
    Write-Host "[4] Version set to $version" -ForegroundColor Cyan
}

Write-Host "[5] Pushing to GitHub..." -ForegroundColor Cyan
Push-Location "$SERVER"
git add mod.jar.enc
$commitMsg = "deploy: update mod.jar.enc$(if ($version) { " v$version" } else { " $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" })"
git commit -m $commitMsg
if ($LASTEXITCODE -ne 0) {
    Write-Host "[5] No changes, forcing redeploy..." -ForegroundColor Yellow
    git commit --allow-empty -m "redeploy: trigger Railway $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
}
git push
Pop-Location

Write-Host "DONE! Railway will auto-deploy in ~2 minutes." -ForegroundColor Green
if ($version -ne "") {
    Write-Host "Don't forget to set JAR_VERSION=$version in Railway." -ForegroundColor Yellow
}
