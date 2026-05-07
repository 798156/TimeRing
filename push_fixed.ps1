$token = "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"
$owner = "798156"
$repo = "TimeRing"
$repoPath = "d:\Android_Kotlin"
$apiBase = "https://api.github.com/repos/$owner/$repo"
$headers = @{
    Authorization = "token $token"
    "Content-Type" = "application/json"
}

# First, create an initial file (README) to bootstrap the repo
Write-Host "Creating initial README to bootstrap repo..."
$readmeContent = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("# TimeRing - 时环`nInitial commit placeholder"))
$putBody = @{
    message = "Initial commit"
    content = $readmeContent
    branch = "main"
} | ConvertTo-Json
try {
    $result = Invoke-RestMethod -Uri "$apiBase/contents/README.md" -Method Put -Headers $headers -Body $putBody
    Write-Host "  README created, commit: $($result.commit.sha.Substring(0,7))"
} catch {
    Write-Host "  README create result: $_"
}
Start-Sleep -Seconds 2

# Now get the current HEAD
Write-Host "Getting current HEAD..."
$ref = Invoke-RestMethod -Uri "$apiBase/git/refs/heads/main" -Headers $headers
$baseCommitSha = $ref.object.sha
Write-Host "  Base commit: $baseCommitSha"
$baseCommit = Invoke-RestMethod -Uri "$apiBase/git/commits/$baseCommitSha" -Headers $headers
$baseTreeSha = $baseCommit.tree.sha

# Collect files
Write-Host "Collecting files..."
$ignoreDirs = @(
    ".git", ".gradle", ".idea", ".kotlin", "build", "app/build"
)
$ignorePatterns = @("*.iml", ".DS_Store", "local.properties", "*.log", "qemu-*.log")

$files = @()
Get-ChildItem $repoPath -Recurse -File | ForEach-Object {
    $relPath = $_.FullName.Substring($repoPath.Length + 1).Replace("\", "/")
    foreach ($dir in $ignoreDirs) {
        if ($relPath -eq $dir -or $relPath.StartsWith("$dir/")) { return }
    }
    foreach ($pattern in $ignorePatterns) {
        if ($relPath -like $pattern -or $relPath -like "*/$pattern") { return }
    }
    if ($relPath -ne "README.md" -and $relPath -like "*.apk" -and -not ($relPath -like "releases/*.apk")) { return }
    $files += @{ Path = $_.FullName; RelPath = $relPath }
}
Write-Host "Found $($files.Count) files to upload"

# Create blobs
Write-Host "Creating blobs..."
$treeItems = @()
$i = 0
foreach ($file in $files) {
    $i++
    $content = [System.IO.File]::ReadAllBytes($file.Path)
    $isBinary = $file.RelPath -match "\.(apk|png|webp|jar|ico)$"
    
    if ($isBinary) {
        $encoded = [Convert]::ToBase64String($content)
        $blobBody = @{ content = $encoded; encoding = "base64" } | ConvertTo-Json
    } else {
        $text = [System.Text.Encoding]::UTF8.GetString($content)
        $blobBody = @{ content = $text; encoding = "utf-8" } | ConvertTo-Json
    }
    
    try {
        $blob = Invoke-RestMethod -Uri "$apiBase/git/blobs" -Method Post -Headers $headers -Body $blobBody
        $treeItems += @{ path = $file.RelPath; mode = "100644"; type = "blob"; sha = $blob.sha }
        Write-Host "  [$i/$($files.Count)] $($file.RelPath)"
    } catch {
        Write-Host "  ERROR [$i/$($files.Count)] $($file.RelPath): $_"
    }
}

# Create tree
Write-Host "Creating tree..."
$treeBody = @{ tree = $treeItems } | ConvertTo-Json -Depth 10
$newTree = Invoke-RestMethod -Uri "$apiBase/git/trees" -Method Post -Headers $headers -Body $treeBody
Write-Host "  Tree SHA: $($newTree.sha)"

# Create commit
Write-Host "Creating commit..."
$commitBody = @{
    message = "Initial commit: 时环 (TimeRing) - 专注学习助手"
    tree = $newTree.sha
    parents = @($baseCommitSha)
} | ConvertTo-Json -Depth 5
$commit = Invoke-RestMethod -Uri "$apiBase/git/commits" -Method Post -Headers $headers -Body $commitBody
Write-Host "  Commit SHA: $($commit.sha)"

# Update ref
Write-Host "Updating main branch..."
$refBody = @{ sha = $commit.sha; force = $true } | ConvertTo-Json
$updatedRef = Invoke-RestMethod -Uri "$apiBase/git/refs/heads/main" -Method Patch -Headers $headers -Body $refBody
Write-Host "SUCCESS! All files uploaded!"
Write-Host "Repository: https://github.com/$owner/$repo"
