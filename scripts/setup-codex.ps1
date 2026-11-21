$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$skillsRoot = Join-Path $projectRoot '.claude/skills'

foreach ($skillDirectory in Get-ChildItem -LiteralPath $skillsRoot -Directory) {
  $source = $skillDirectory.FullName
  if (-not (Test-Path -LiteralPath (Join-Path $source 'SKILL.md') -PathType Leaf)) {
    continue
  }
  $link = Join-Path $projectRoot ('.agents/skills/' + $skillDirectory.Name)

  $existing = Get-Item -LiteralPath $link -Force -ErrorAction SilentlyContinue
  if ($existing) {
    if ($existing.LinkType -notin 'Junction', 'SymbolicLink') {
      throw 'The skill path already exists and is not a directory link; nothing was overwritten.'
    }
    $targetPath = @($existing.Target)[0]
    if (-not [System.IO.Path]::IsPathRooted($targetPath)) {
      $targetPath = Join-Path (Split-Path -Parent $link) $targetPath
    }
    $target = [System.IO.Path]::GetFullPath($targetPath)
    if ($target -ne [System.IO.Path]::GetFullPath($source)) {
      throw 'The existing skill link points elsewhere; nothing was overwritten.'
    }
  } else {
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $link) | Out-Null
    $linkType = if ($env:OS -eq 'Windows_NT') { 'Junction' } else { 'SymbolicLink' }
    New-Item -ItemType $linkType -Path $link -Value $source | Out-Null
  }
}

Write-Output 'Project skills are linked to their canonical directories. No copies were created.'
