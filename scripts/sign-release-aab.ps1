#Requires -Version 5.1
<#
.SYNOPSIS
  Locally sign DZlog Android v1.0.0 (versionCode 3) AAB with the reset Play upload key.
.DESCRIPTION
  Runs only on the user's own computer. Never uploads the keystore or reads
  the password into this script; keytool/jarsigner prompt interactively.
  Checks the public upload certificate SHA-256 before signing.
.EXAMPLE
  .\scripts\sign-release-aab.ps1 -UnsignedAab "C:\Users\me\Downloads\app-release.aab"
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $UnsignedAab,
    [string] $KeyStore = (Join-Path $HOME "Documents\DZlog-Signing-Private\dzlog-upload-v1.jks"),
    [string] $OutputAab = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$expectedFingerprint = "02:E6:59:D3:37:C4:E2:63:97:E4:0A:43:54:C7:AD:1B:C5:2A:EF:88:EF:33:26:33:00:89:7D:A3:93:9B:D8:00"

if (-not (Test-Path -LiteralPath $UnsignedAab -PathType Leaf)) {
    throw "Cannot find input AAB: $UnsignedAab"
}
if (-not (Test-Path -LiteralPath $KeyStore -PathType Leaf)) {
    throw "Cannot find private keystore on this computer: $KeyStore"
}
foreach ($commandName in @("keytool", "jarsigner")) {
    if (-not (Get-Command $commandName -ErrorAction SilentlyContinue)) {
        throw "$commandName is unavailable. Install/use JDK 21 and check PATH."
    }
}

$UnsignedAab = (Resolve-Path -LiteralPath $UnsignedAab).Path
$KeyStore = (Resolve-Path -LiteralPath $KeyStore).Path
if (-not $UnsignedAab.EndsWith(".aab", [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "The input must be an Android App Bundle (.aab)."
}
if ([string]::IsNullOrWhiteSpace($OutputAab)) {
    $OutputAab = Join-Path (Split-Path -Parent $UnsignedAab) "DZlog-v1.0.0-vc3-signed.aab"
}
$OutputAab = [System.IO.Path]::GetFullPath($OutputAab)
if (Test-Path -LiteralPath $OutputAab) {
    throw "Output already exists. Move/rename it before signing: $OutputAab"
}
if ($UnsignedAab -eq $OutputAab) {
    throw "The signed output must not overwrite the unsigned input."
}

# The DER certificate contains only the public key. Delete this temporary copy.
$certPath = Join-Path ([System.IO.Path]::GetTempPath()) ("dzlog-upload-" + [guid]::NewGuid().ToString("N") + ".cer")
try {
    Write-Host "Checking local Play upload certificate. The JDK will prompt for the keystore password."
    & keytool -exportcert -alias upload -storetype PKCS12 -keystore $KeyStore -file $certPath
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $certPath -PathType Leaf)) {
        throw "Could not export the public upload certificate. No AAB was signed."
    }
    $certificate = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2($certPath)
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        $digest = $sha256.ComputeHash($certificate.RawData)
        $actualFingerprint = ($digest | ForEach-Object { $_.ToString("X2") }) -join ":"
    } finally {
        $sha256.Dispose()
        $certificate.Reset()
    }
    if ($actualFingerprint -cne $expectedFingerprint) {
        throw "Upload certificate mismatch. Expected $expectedFingerprint ; found $actualFingerprint . No signing attempted."
    }
    Write-Host "Upload certificate fingerprint matches the reset key."
} finally {
    if (Test-Path -LiteralPath $certPath) {
        Remove-Item -LiteralPath $certPath -Force
    }
}

Write-Host "Signing with alias 'upload'. Enter passwords only into the local JDK prompts."
& jarsigner -keystore $KeyStore -storetype PKCS12 -digestalg SHA-256 -sigalg SHA256withRSA -signedjar $OutputAab $UnsignedAab upload
if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $OutputAab -PathType Leaf)) {
    throw "jarsigner did not produce a signed AAB."
}

Write-Host "Verifying the signed AAB..."
& jarsigner -verify -verbose -certs $OutputAab
if ($LASTEXITCODE -ne 0) {
    throw "Signed AAB verification failed. Do not upload this file to Google Play."
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($OutputAab)
try {
    $signatures = @($archive.Entries | Where-Object {
        $_.FullName -match '^META-INF/[^/]+\.(RSA|DSA|EC)$'
    })
    if ($signatures.Count -eq 0) {
        throw "No JAR signature block found in the AAB. Do not upload."
    }
} finally {
    $archive.Dispose()
}

$hash = Get-FileHash -LiteralPath $OutputAab -Algorithm SHA256
Write-Host "SUCCESS: signed and verified AAB ready for Play internal testing."
Write-Host "File: $OutputAab"
Write-Host "SHA-256: $($hash.Hash)"
Write-Host "Do not upload the .jks, passwords or the unsigned app-release.aab."
