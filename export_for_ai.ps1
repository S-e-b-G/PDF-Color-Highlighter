$ErrorActionPreference = 'Stop'

# ➡️ Folder to export, relative to this PowerShell script.
#   - Subdirectory
# $project = Join-Path $PSScriptRoot 'PdfColorHighlighter-M2c.1-update'
#   - Terminal's current directory
$project = (Get-Location).Path

# Python exporter, stored alongside this PowerShell script.
$exporter = Join-Path $PSScriptRoot 'export_for_ai.py'

# Local date/time, including milliseconds.
$timestamp = Get-Date -Format 'yyyy-MM-dd_HH-mm-ss-fff'

# Save exports alongside this PowerShell script.
$output = Join-Path $PSScriptRoot "export_for_ai_$timestamp.md"

# ➡️ Add, remove, or comment out exclusions.
# Names match at any depth; relative paths use forward slashes.
$excludedPatterns = @(
    '.git'
	'.gitignore'
    '.gradle'
    '.idea'
    '.kotlin'
    '.validation'
    'build'
	'export_for_ai*'
    'local.properties'
	'm1-*'
	'M1-results*'
    'project_for_ai*.md'  # Avoid including previous exports.
    'SG-INFOS'
)

# Build the Python argument list.
$pythonArgs = @(
    $exporter
    $project
    '-o'
    $output
    '--max-file-mb'
    '0'
)

foreach ($pattern in $excludedPatterns) {
    $pythonArgs += '--exclude', $pattern
}

# Run the export.
py @pythonArgs

if ($LASTEXITCODE -ne 0) {
    throw 'Export failed. See the Python error above.'
}

Write-Host "Export ready: $output"

Pause