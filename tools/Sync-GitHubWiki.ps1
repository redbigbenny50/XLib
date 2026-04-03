[CmdletBinding()]
param(
    [string]$WikiRemoteUrl,
    [string]$OutputDir,
    [string]$Branch = "master",
    [switch]$Push
)

$ErrorActionPreference = "Stop"

function Get-RepoRoot {
    $root = git -C $PSScriptRoot rev-parse --show-toplevel 2>$null
    if (-not $root) {
        throw "Unable to resolve the repository root."
    }
    return $root.Trim()
}

function Get-RepoHttpUrl {
    param([string]$OriginUrl)

    if (-not $OriginUrl) {
        throw "Unable to resolve remote.origin.url."
    }

    $trimmed = $OriginUrl.Trim()
    if ($trimmed -match '^https://github\.com/(?<owner>[^/]+)/(?<repo>[^/.]+?)(?:\.git)?/?$') {
        return "https://github.com/$($matches.owner)/$($matches.repo)"
    }
    if ($trimmed -match '^git@github\.com:(?<owner>[^/]+)/(?<repo>[^/.]+?)(?:\.git)?$') {
        return "https://github.com/$($matches.owner)/$($matches.repo)"
    }

    throw "Remote origin '$trimmed' is not a supported GitHub URL."
}

function Convert-MarkdownForWiki {
    param(
        [string]$Content,
        [string]$RepoHttpUrl
    )

    $pattern = '\[(?<text>[^\]]+)\]\((?<target>[^)]+)\)'
    return [regex]::Replace($Content, $pattern, {
        param($match)

        $text = $match.Groups["text"].Value
        $target = $match.Groups["target"].Value
        $rewritten = $target

        switch -Regex ($target) {
            '^\.\./README\.md$' {
                $rewritten = "$RepoHttpUrl/blob/main/README.md"
                break
            }
            '^CODEBASE_MAP\.md$' {
                $rewritten = "$RepoHttpUrl/blob/main/docs/CODEBASE_MAP.md"
                break
            }
            '^wiki/(?<page>[^/]+?)\.md$' {
                $rewritten = $matches.page
                break
            }
            '^(?<page>[^/]+?)\.md$' {
                $rewritten = $matches.page
                break
            }
        }

        return "[$text]($rewritten)"
    })
}

function New-EmptyDirectory {
    param([string]$Path)

    if (Test-Path $Path) {
        Get-ChildItem -Path $Path -Force | Remove-Item -Recurse -Force
    } else {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Write-WikiFiles {
    param(
        [string]$TargetDir,
        [string]$RepoRoot,
        [string]$RepoHttpUrl
    )

    $guidePath = Join-Path $RepoRoot "docs/XLIB_USAGE_GUIDE.md"
    $wikiSourceDir = Join-Path $RepoRoot "docs/wiki"
    $pageOrder = @(
        "Getting-Started.md",
        "Abilities-and-Loadouts.md",
        "Modes-and-Combos.md",
        "Grants-Items-and-Recipes.md",
        "Progression.md",
        "Events-Commands-and-Testing.md"
    )

    foreach ($markdownFile in Get-ChildItem -Path $TargetDir -File -Filter "*.md" -Force) {
        Remove-Item -Force $markdownFile.FullName
    }

    $homeContent = Get-Content -Raw -Path $guidePath
    $homeContent = Convert-MarkdownForWiki -Content $homeContent -RepoHttpUrl $RepoHttpUrl
    Set-Content -Path (Join-Path $TargetDir "Home.md") -Value $homeContent

    foreach ($page in $pageOrder) {
        $sourcePath = Join-Path $wikiSourceDir $page
        $content = Get-Content -Raw -Path $sourcePath
        $content = Convert-MarkdownForWiki -Content $content -RepoHttpUrl $RepoHttpUrl
        Set-Content -Path (Join-Path $TargetDir $page) -Value $content
    }

    $sidebar = @(
        "# XLib Wiki",
        "",
        "- [Home](Home)",
        "- [Getting Started](Getting-Started)",
        "- [Abilities and Loadouts](Abilities-and-Loadouts)",
        "- [Modes and Combos](Modes-and-Combos)",
        "- [Grants, Items, and Recipes](Grants-Items-and-Recipes)",
        "- [Progression](Progression)",
        "- [Events, Commands, and Testing](Events-Commands-and-Testing)",
        "",
        "## Repo Docs",
        "",
        "- [README]($RepoHttpUrl/blob/main/README.md)",
        "- [Codebase Map]($RepoHttpUrl/blob/main/docs/CODEBASE_MAP.md)"
    ) -join "`r`n"
    Set-Content -Path (Join-Path $TargetDir "_Sidebar.md") -Value $sidebar
}

$repoRoot = Get-RepoRoot
$originUrl = git -C $repoRoot config --get remote.origin.url
$repoHttpUrl = Get-RepoHttpUrl -OriginUrl $originUrl

if (-not $WikiRemoteUrl) {
    $WikiRemoteUrl = "$repoHttpUrl.wiki.git"
}

if (-not $Push -and -not $OutputDir) {
    throw "Specify -OutputDir for a local export or use -Push to publish to the GitHub wiki."
}

if ($Push) {
    $tempDir = Join-Path $env:TEMP ("xlib-github-wiki-" + [Guid]::NewGuid().ToString("N"))
    try {
        git clone $WikiRemoteUrl $tempDir | Out-Null
    } catch {
        throw "Unable to clone the wiki repo at '$WikiRemoteUrl'. GitHub wiki repos do not materialize until the first page is created in the GitHub web UI once."
    }

    Write-WikiFiles -TargetDir $tempDir -RepoRoot $repoRoot -RepoHttpUrl $repoHttpUrl

    if (git -C $tempDir status --porcelain) {
        git -C $tempDir add -A
        git -C $tempDir -c user.name="$(git -C $repoRoot config user.name)" -c user.email="$(git -C $repoRoot config user.email)" commit -m "Sync wiki from docs"
        git -C $tempDir push origin "HEAD:$Branch"
    } else {
        Write-Host "No wiki changes to push."
    }

    Write-Host "GitHub wiki sync completed."
    return
}

New-EmptyDirectory -Path $OutputDir
Write-WikiFiles -TargetDir $OutputDir -RepoRoot $repoRoot -RepoHttpUrl $repoHttpUrl
Write-Host "Wiki files exported to $OutputDir"
