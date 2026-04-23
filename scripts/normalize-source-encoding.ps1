Param()

$utf8Strict = New-Object System.Text.UTF8Encoding($false, $true)
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$patterns = @('*.vue', '*.ts', '*.tsx', '*.js', '*.mjs', '*.css', '*.scss', '*.html', '*.java', '*.xml', '*.sql', '*.properties')
$roots = @('frontend', 'backend')

foreach ($root in $roots) {
  if (-not (Test-Path $root)) {
    continue
  }

  $files = Get-ChildItem -Path $root -Recurse -File -Include $patterns
  foreach ($file in $files) {
    try {
      $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
      $text = $utf8Strict.GetString($bytes)
      [System.IO.File]::WriteAllText($file.FullName, $text, $utf8NoBom)
    } catch {
      Write-Warning "Skipped: $($file.FullName)"
    }
  }
}

Write-Output 'Source files normalized to UTF-8 without BOM.'
