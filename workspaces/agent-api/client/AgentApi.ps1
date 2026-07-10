# Agent Portal Agent API PowerShell client.
# Dot-source this file to load functions into your session:
#   . .\AgentApi.ps1
#
# Domain (default — delena.buzz, not raw public IP):
#   Connect-AgentApi -Username admin -Password '<password>'
# Local host stack:
#   Connect-AgentApi -Local -Username admin -Password '<password>'

$script:PublicApiBase = 'https://delena.buzz/api'
$script:PublicCssBase = 'https://delena.buzz'
$script:LocalApiBase = 'http://127.0.0.1:8080/api'
$script:LocalCssBase = 'http://127.0.0.1:9000'

$script:BaseUrl = $script:PublicApiBase
$script:CssUrl = $script:PublicCssBase
$script:Token = $null
$script:ApiKey = $null
$script:ClientId = 'agent-portal'

function ConvertTo-AgentQueryString {
    param(
        [Parameter(Mandatory = $false)]
        [hashtable]$Query
    )

    if (-not $Query) {
        return ''
    }

    $parts = New-Object System.Collections.Generic.List[string]
    foreach ($key in $Query.Keys) {
        $value = $Query[$key]
        if ($null -eq $value) {
            continue
        }

        if ($value -is [System.Array]) {
            foreach ($item in $value) {
                if ($null -ne $item) {
                    $parts.Add(('{0}={1}' -f [Uri]::EscapeDataString([string]$key), [Uri]::EscapeDataString([string]$item)))
                }
            }
        } else {
            $parts.Add(('{0}={1}' -f [Uri]::EscapeDataString([string]$key), [Uri]::EscapeDataString([string]$value)))
        }
    }

    if ($parts.Count -eq 0) {
        return ''
    }

    return '?' + ($parts -join '&')
}

function Join-AgentUrl {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Base,

        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $false)]
        [hashtable]$Query
    )

    $left = $Base.TrimEnd('/')
    $right = $Path.TrimStart('/')
    return ('{0}/{1}{2}' -f $left, $right, (ConvertTo-AgentQueryString -Query $Query))
}

function Get-AgentCssLoginUrl {
    param(
        [Parameter(Mandatory = $true)]
        [string]$CssUrl
    )

    $trimmed = $CssUrl.TrimEnd('/')
    $uri = $null
    if ([Uri]::TryCreate($trimmed, [UriKind]::Absolute, [ref]$uri) -and $uri.AbsolutePath.TrimEnd('/') -ieq '/auth') {
        return (Join-AgentUrl -Base $trimmed -Path '/login')
    }

    return (Join-AgentUrl -Base $trimmed -Path '/auth/login')
}

function Set-AgentApiConfig {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $false)]
        [string]$BaseUrl,

        [Parameter(Mandatory = $false)]
        [string]$CssUrl,

        [Parameter(Mandatory = $false)]
        [string]$ClientId,

        [Parameter(Mandatory = $false)]
        [switch]$Local,

        [Parameter(Mandatory = $false)]
        [switch]$Domain
    )

    if ($Local) {
        $script:BaseUrl = $script:LocalApiBase
        $script:CssUrl = $script:LocalCssBase
    } elseif ($Domain) {
        $script:BaseUrl = $script:PublicApiBase
        $script:CssUrl = $script:PublicCssBase
    }

    if ($PSBoundParameters.ContainsKey('BaseUrl')) {
        $script:BaseUrl = $BaseUrl.TrimEnd('/')
    }
    if ($PSBoundParameters.ContainsKey('CssUrl')) {
        $script:CssUrl = $CssUrl.TrimEnd('/')
    }
    if ($PSBoundParameters.ContainsKey('ClientId')) {
        $script:ClientId = $ClientId
    }

    [pscustomobject]@{
        BaseUrl = $script:BaseUrl
        CssUrl = $script:CssUrl
        ClientId = $script:ClientId
        AuthMode = if ($script:Token) { 'Bearer' } elseif ($script:ApiKey) { 'ApiKey' } else { 'None' }
    }
}

function Get-AgentApiConfig {
    [CmdletBinding()]
    param()

    [pscustomobject]@{
        BaseUrl = $script:BaseUrl
        CssUrl = $script:CssUrl
        ClientId = $script:ClientId
        AuthMode = if ($script:Token) { 'Bearer' } elseif ($script:ApiKey) { 'ApiKey' } else { 'None' }
        HasToken = [bool]$script:Token
        HasApiKey = [bool]$script:ApiKey
    }
}

function Connect-AgentApi {
    [CmdletBinding(DefaultParameterSetName = 'Css')]
    param(
        [Parameter(Mandatory = $true, ParameterSetName = 'Css')]
        [string]$Username,

        [Parameter(Mandatory = $true, ParameterSetName = 'Css')]
        [string]$Password,

        [Parameter(Mandatory = $true, ParameterSetName = 'ApiKey')]
        [string]$ApiKey,

        [Parameter(Mandatory = $false)]
        [string]$CssUrl = $script:CssUrl,

        [Parameter(Mandatory = $false)]
        [string]$BaseUrl = $script:BaseUrl,

        [Parameter(Mandatory = $false)]
        [string]$ClientId = $script:ClientId,

        [Parameter(Mandatory = $false)]
        [switch]$Local,

        [Parameter(Mandatory = $false)]
        [switch]$Domain
    )

    if ($Local) {
        $BaseUrl = $script:LocalApiBase
        $CssUrl = $script:LocalCssBase
    } elseif ($Domain) {
        $BaseUrl = $script:PublicApiBase
        $CssUrl = $script:PublicCssBase
    }

    $script:BaseUrl = $BaseUrl.TrimEnd('/')
    $script:CssUrl = $CssUrl.TrimEnd('/')
    $script:ClientId = $ClientId

    if ($PSCmdlet.ParameterSetName -eq 'ApiKey') {
        $script:ApiKey = $ApiKey
        $script:Token = $null
        return [pscustomobject]@{
            BaseUrl = $script:BaseUrl
            CssUrl = $script:CssUrl
            ClientId = $script:ClientId
            AuthMode = 'ApiKey'
        }
    }

    $body = [ordered]@{
        username = $Username
        password = $Password
        clientId = $ClientId
    }

    $loginUrl = Get-AgentCssLoginUrl -CssUrl $script:CssUrl
    # Avoid PowerShell/curl escaping pitfalls: write UTF-8 JSON bytes
    $json = ($body | ConvertTo-Json -Compress -Depth 8)
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    $headers = @{
        Origin = if ($script:CssUrl -match '^https://') { $script:CssUrl } else { 'http://127.0.0.1:4200' }
    }
    $response = Invoke-RestMethod -Method Post -Uri $loginUrl -ContentType 'application/json; charset=utf-8' -Body $bytes -Headers $headers

    if (-not $response.accessToken) {
        throw "CSS login did not return an accessToken."
    }

    $script:Token = $response.accessToken
    $script:ApiKey = $null

    [pscustomobject]@{
        BaseUrl = $script:BaseUrl
        CssUrl = $script:CssUrl
        ClientId = $script:ClientId
        AuthMode = 'Bearer'
        Username = $Username
        ExpiresIn = $response.expiresIn
    }
}

function Disconnect-AgentApi {
    [CmdletBinding()]
    param()

    $script:Token = $null
    $script:ApiKey = $null
    [pscustomobject]@{
        BaseUrl = $script:BaseUrl
        AuthMode = 'None'
    }
}

function Invoke-AgentApi {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet('GET', 'POST', 'PUT', 'PATCH', 'DELETE')]
        [string]$Method,

        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $false)]
        [hashtable]$Query,

        [Parameter(Mandatory = $false)]
        [object]$Body,

        [Parameter(Mandatory = $false)]
        [hashtable]$Headers
    )

    $uri = Join-AgentUrl -Base $script:BaseUrl -Path $Path -Query $Query
    $requestHeaders = @{}
    if ($Headers) {
        foreach ($key in $Headers.Keys) {
            $requestHeaders[$key] = $Headers[$key]
        }
    }

    if ($script:Token) {
        $requestHeaders['Authorization'] = "Bearer $script:Token"
    } elseif ($script:ApiKey) {
        $requestHeaders['X-API-Key'] = $script:ApiKey
    }

    $parameters = @{
        Method = $Method
        Uri = $uri
        Headers = $requestHeaders
    }

    if ($PSBoundParameters.ContainsKey('Body')) {
        $parameters['ContentType'] = 'application/json'
        $parameters['Body'] = ($Body | ConvertTo-Json -Depth 20)
    }

    try {
        Invoke-RestMethod @parameters
    } catch {
        $message = $_.Exception.Message
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            $statusDescription = $_.Exception.Response.StatusDescription
            $message = "Agent API request failed: HTTP $statusCode $statusDescription. $message"
        }
        throw $message
    }
}

function Get-AgentHealth {
    [CmdletBinding()]
    param()
    Invoke-AgentApi -Method GET -Path '/health'
}

function Get-AgentAuthConfig {
    [CmdletBinding()]
    param()
    Invoke-AgentApi -Method GET -Path '/auth/config'
}

function Get-AgentPresets {
    [CmdletBinding()]
    param()
    Invoke-AgentApi -Method GET -Path '/presets'
}

function Get-AgentSessions {
    [CmdletBinding()]
    param()
    Invoke-AgentApi -Method GET -Path '/sessions'
}

function Get-AgentSession {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId
    )
    Invoke-AgentApi -Method GET -Path "/sessions/$SessionId"
}

function New-AgentSession {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $false)]
        [string]$Title = 'Agent API session',

        [Parameter(Mandatory = $false)]
        [string]$WorkspacePath = 'agent-api',

        [Parameter(Mandatory = $false)]
        [string]$Provider = 'cursor',

        [Parameter(Mandatory = $false)]
        [bool]$UseGuidanceDefaults = $true
    )

    $body = @{
        title = $Title
        workspacePath = $WorkspacePath
        provider = $Provider
        useGuidanceDefaults = $UseGuidanceDefaults
    }
    Invoke-AgentApi -Method POST -Path '/sessions' -Body $body
}

function Send-AgentPrompt {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId,

        [Parameter(Mandatory = $true)]
        [string]$Prompt
    )

    Invoke-AgentApi -Method POST -Path "/sessions/$SessionId/prompt" -Body @{ prompt = $Prompt }
}

function Get-AgentMessages {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId
    )
    Invoke-AgentApi -Method GET -Path "/sessions/$SessionId/messages"
}

function Get-AgentTools {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId
    )
    Invoke-AgentApi -Method GET -Path "/sessions/$SessionId/tools"
}

function Get-AgentPermissions {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId
    )
    Invoke-AgentApi -Method GET -Path "/sessions/$SessionId/permissions"
}

function Resolve-AgentPermission {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId,

        [Parameter(Mandatory = $true)]
        [string]$PermissionId,

        [Parameter(Mandatory = $true)]
        [ValidateSet('accept', 'allow-once', 'allow-always', 'reject', 'reject-once', 'deny', 'always')]
        [string]$Decision,

        [Parameter(Mandatory = $false)]
        [string]$Reason
    )

    $body = @{ decision = $Decision }
    if ($PSBoundParameters.ContainsKey('Reason')) {
        $body['reason'] = $Reason
    }

    Invoke-AgentApi -Method POST -Path "/sessions/$SessionId/permissions/$PermissionId" -Body $body
}

function Stop-AgentRun {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId
    )
    Invoke-AgentApi -Method POST -Path "/sessions/$SessionId/cancel" -Body @{}
}

function Abandon-AgentSubagent {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId,

        [Parameter(Mandatory = $true)]
        [string]$SubagentId
    )

    $encodedSubagentId = [Uri]::EscapeDataString($SubagentId)
    Invoke-AgentApi -Method POST -Path "/sessions/$SessionId/subagents/$encodedSubagentId/abandon" -Body @{}
}

function Get-AgentFiles {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId,

        [Parameter(Mandatory = $false)]
        [string]$Path = ''
    )

    $query = @{}
    if ($Path) {
        $query['path'] = $Path
    }

    Invoke-AgentApi -Method GET -Path "/sessions/$SessionId/files" -Query $query
}

function Get-AgentFileContent {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId,

        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    Invoke-AgentApi -Method GET -Path "/sessions/$SessionId/files/content" -Query @{ path = $Path }
}

function Get-AgentChanges {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId
    )
    Invoke-AgentApi -Method GET -Path "/sessions/$SessionId/changes"
}

function Get-AgentChangeDiff {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId,

        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    Invoke-AgentApi -Method GET -Path "/sessions/$SessionId/changes/diff" -Query @{ path = $Path }
}

function Keep-AgentChange {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId,

        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    Invoke-AgentApi -Method POST -Path "/sessions/$SessionId/changes/accept" -Body @{ path = $Path }
}

function Restore-AgentChange {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId,

        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    Invoke-AgentApi -Method POST -Path "/sessions/$SessionId/changes/reject" -Body @{ path = $Path }
}

function Get-AgentEvents {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId
    )
    Invoke-AgentApi -Method GET -Path "/sessions/$SessionId/events"
}

function Archive-AgentSession {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId
    )
    Invoke-AgentApi -Method POST -Path "/sessions/$SessionId/archive" -Body @{}
}

function Unarchive-AgentSession {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId
    )
    Invoke-AgentApi -Method POST -Path "/sessions/$SessionId/unarchive" -Body @{}
}

function Get-AgentCollaborators {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId
    )
    Invoke-AgentApi -Method GET -Path "/sessions/$SessionId/collaborators"
}

function Add-AgentCollaborator {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId,

        [Parameter(Mandatory = $true)]
        [string]$Username
    )

    Invoke-AgentApi -Method POST -Path "/sessions/$SessionId/collaborators" -Body @{ username = $Username }
}

function Remove-AgentCollaborator {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId,

        [Parameter(Mandatory = $true)]
        [string]$Username
    )

    $encodedUsername = [Uri]::EscapeDataString($Username)
    Invoke-AgentApi -Method DELETE -Path "/sessions/$SessionId/collaborators/$encodedUsername"
}

function Get-AgentGuidance {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId
    )
    Invoke-AgentApi -Method GET -Path "/sessions/$SessionId/guidance"
}

function Set-AgentGuidance {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId,

        [Parameter(Mandatory = $true)]
        [object[]]$Items
    )

    Invoke-AgentApi -Method PUT -Path "/sessions/$SessionId/guidance" -Body @{ items = $Items }
}

function Get-AgentGuidancePacks {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $false)]
        [ValidateSet('RULE', 'SKILL', 'rule', 'skill')]
        [string]$Kind
    )

    $query = @{}
    if ($Kind) {
        $query['kind'] = $Kind.ToUpperInvariant()
    }

    Invoke-AgentApi -Method GET -Path '/guidance/packs' -Query $query
}

function New-AgentGuidancePack {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet('RULE', 'SKILL', 'rule', 'skill')]
        [string]$Kind,

        [Parameter(Mandatory = $true)]
        [string]$Title,

        [Parameter(Mandatory = $true)]
        [string]$BodyMarkdown,

        [Parameter(Mandatory = $false)]
        [string]$Description,

        [Parameter(Mandatory = $false)]
        [string]$Globs,

        [Parameter(Mandatory = $false)]
        [bool]$AlwaysApply = $false,

        [Parameter(Mandatory = $false)]
        [bool]$EnabledByDefault = $false,

        [Parameter(Mandatory = $false)]
        [string]$Slug
    )

    $body = @{
        kind = $Kind.ToUpperInvariant()
        title = $Title
        bodyMarkdown = $BodyMarkdown
        alwaysApply = $AlwaysApply
        enabledByDefault = $EnabledByDefault
    }
    if ($PSBoundParameters.ContainsKey('Description')) { $body['description'] = $Description }
    if ($PSBoundParameters.ContainsKey('Globs')) { $body['globs'] = $Globs }
    if ($PSBoundParameters.ContainsKey('Slug')) { $body['slug'] = $Slug }

    Invoke-AgentApi -Method POST -Path '/guidance/packs' -Body $body
}

function Set-AgentGuidancePack {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$PackId,

        [Parameter(Mandatory = $true)]
        [hashtable]$Body
    )

    Invoke-AgentApi -Method PATCH -Path "/guidance/packs/$PackId" -Body $Body
}

function Remove-AgentGuidancePack {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$PackId
    )
    Invoke-AgentApi -Method DELETE -Path "/guidance/packs/$PackId"
}

function Get-AgentGuidanceDefaults {
    [CmdletBinding()]
    param()
    Invoke-AgentApi -Method GET -Path '/guidance/defaults'
}

function Set-AgentGuidanceDefaults {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$EnabledPackIds
    )
    Invoke-AgentApi -Method PUT -Path '/guidance/defaults' -Body @{ enabledPackIds = $EnabledPackIds }
}

function Get-AgentGuidanceTemplates {
    [CmdletBinding()]
    param()
    Invoke-AgentApi -Method GET -Path '/guidance/templates'
}

function Install-AgentGuidanceTemplates {
    [CmdletBinding()]
    param()
    Invoke-AgentApi -Method POST -Path '/guidance/templates/install' -Body @{}
}

function Get-AgentAudit {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $false)]
        [string]$SessionId,

        [Parameter(Mandatory = $false)]
        [int]$Limit = 50
    )

    $query = @{ limit = $Limit }
    if ($SessionId) {
        $query['sessionId'] = $SessionId
    }
    Invoke-AgentApi -Method GET -Path '/audit' -Query $query
}

function Show-AgentApiUsage {
    [CmdletBinding()]
    param()

    @'
Agent Portal Agent API PowerShell client

Dot-source this file before calling functions:
  . .\AgentApi.ps1

CSS bearer-token login (defaults to https://delena.buzz — not public IP):
  Connect-AgentApi -Username "<username>" -Password "<password>"

Local host stack:
  Connect-AgentApi -Local -Username "<username>" -Password "<password>"

API-key mode:
  Connect-AgentApi -ApiKey "<api-key>"

Basic flow:
  Get-AgentHealth
  $session = New-AgentSession -Title "API Bridge"
  Send-AgentPrompt -SessionId $session.id -Prompt "Summarize this workspace"
  Get-AgentMessages -SessionId $session.id
  Get-AgentChanges -SessionId $session.id

Useful review actions:
  Get-AgentChangeDiff -SessionId $session.id -Path "README.md"
  Keep-AgentChange -SessionId $session.id -Path "README.md"
  Restore-AgentChange -SessionId $session.id -Path "README.md"

Permission actions:
  Get-AgentPermissions -SessionId $session.id
  Resolve-AgentPermission -SessionId $session.id -PermissionId "<id>" -Decision allow-once

Configuration:
  Set-AgentApiConfig -BaseUrl "http://127.0.0.1:8080/api" -CssUrl "http://127.0.0.1:9000"
  Get-AgentApiConfig
'@
}

if ($MyInvocation.InvocationName -ne '.') {
    Show-AgentApiUsage
}
