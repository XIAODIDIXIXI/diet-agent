param(
    [ValidateSet('smoke', 'live', 'fault')][string]$Mode = 'smoke',
    [int]$Workers = 3
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$env:PYTHONUTF8 = '1'
if ([string]::IsNullOrWhiteSpace($env:DASHSCOPE_API_KEY)) {
    $env:DASHSCOPE_API_KEY = [Environment]::GetEnvironmentVariable('DASHSCOPE_API_KEY', 'User')
    if ([string]::IsNullOrWhiteSpace($env:DASHSCOPE_API_KEY)) {
        $env:DASHSCOPE_API_KEY = [Environment]::GetEnvironmentVariable('DASHSCOPE_API_KEY', 'Machine')
    }
}
if ($Mode -ne 'fault' -and [string]::IsNullOrWhiteSpace($env:DASHSCOPE_API_KEY)) {
    throw 'Set DASHSCOPE_API_KEY locally before running a real-model evaluation.'
}
$runId = '{0}-{1}-{2}' -f $Mode, (Get-Date -Format 'yyyyMMdd-HHmmss'), ([guid]::NewGuid().ToString('N').Substring(0, 6))
$outputPath = Join-Path $projectRoot "eval/runs/$runId"
& mvn '-q' '-Dtest=QuantitativeEvaluationTest' '-Ddiet.eval=true' "-Ddiet.eval.mode=$Mode" "-Ddiet.eval.workers=$Workers" "-Ddiet.eval.output=$outputPath" 'test'
$testExit = $LASTEXITCODE
if (Test-Path -LiteralPath (Join-Path $outputPath 'observations.jsonl')) {
    & python 'eval/metrics.py' $outputPath
    if ($LASTEXITCODE -ne 0) { throw 'Metric aggregation failed.' }
}
Write-Output "Evaluation evidence: $outputPath"
if ($testExit -ne 0) { throw "Evaluation execution failed (exit $testExit); any partial report retains missing cases as failures." }
