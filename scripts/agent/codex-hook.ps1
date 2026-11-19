$ErrorActionPreference = 'Stop'
$projectRoot = (git rev-parse --show-toplevel)
if ($LASTEXITCODE -ne 0) { throw 'Cannot locate the Git root for the compilation hook.' }
& node (Join-Path $projectRoot 'scripts/agent/compile.mjs')
exit $LASTEXITCODE
