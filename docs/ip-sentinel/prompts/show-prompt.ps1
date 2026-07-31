# show-prompt.ps1 — assemble the shared preamble + a build chunk and copy to clipboard.
# Usage:  .\show-prompt.ps1 0.9      (copies preamble + chunk 0.9 to the clipboard, ready to paste)
#         .\show-prompt.ps1 1.2 -Print   (also prints to stdout)
param(
    [Parameter(Mandatory = $true, Position = 0)][string]$ChunkId,
    [switch]$Print
)
$ErrorActionPreference = 'Stop'

$promptsDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$preamble   = Join-Path $promptsDir '00-shared-preamble.md'
if (-not (Test-Path $preamble)) { Write-Error "Preamble not found: $preamble"; exit 1 }

$chunk = Get-ChildItem -Path $promptsDir -Recurse -Filter "$ChunkId-*.md" | Select-Object -First 1
if (-not $chunk) { Write-Error "No chunk file matching '$ChunkId-*.md' under $promptsDir"; exit 1 }

$text = (Get-Content $preamble -Raw) + "`n`n---`n`n" + (Get-Content $chunk.FullName -Raw)
$text | Set-Clipboard
Write-Host ("Copied preamble + {0} to clipboard ({1:N1} KB). Paste into a fresh Claude Code session on IPSentinel." -f $chunk.Name, ($text.Length / 1KB))
if ($Print) { Write-Output $text }
