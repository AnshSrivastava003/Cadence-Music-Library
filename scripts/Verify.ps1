$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$settings=Get-Content (Join-Path $projectRoot '.local/settings.json') -Raw | ConvertFrom-Json
$base='http://127.0.0.1:8090/api'
$script:passed=0
function Call-Api($Method,$Path,$Token='',$Body=$null) {
    $headers=@{}
    if($Token){$headers.Authorization="Bearer $Token"}
    $args=@{Uri="$base$Path";Method=$Method;Headers=$headers;UseBasicParsing=$true;TimeoutSec=20}
    if($null -ne $Body){$args.ContentType='application/json';$args.Body=($Body|ConvertTo-Json -Depth 8)}
    try {
        $response=Invoke-WebRequest @args
        $data=$null
        if($response.Content){$content=$response.Content;if($content -is [byte[]]){$content=[System.Text.Encoding]::UTF8.GetString($content)};if($content.TrimStart().StartsWith("[")){$data=@();foreach($item in ($content|ConvertFrom-Json)){if($null -ne $item){$data+=,$item}}}else{$data=$content|ConvertFrom-Json}}
        return @{Status=[int]$response.StatusCode;Data=$data}
    } catch {
        if($_.Exception.Response){return @{Status=[int]$_.Exception.Response.StatusCode;Data=$null}}
        throw
    }
}
function Check($Condition,$Name){if(-not $Condition){throw "FAIL: $Name"};$script:passed++;Write-Host "PASS: $Name"}
$stamp=[Guid]::NewGuid().ToString('N').Substring(0,10)
$password='Integration-test-Example-2026'
$first=Call-Api POST '/auth/register' '' @{name='Verification User';email="verify-$stamp@example.test";password=$password;emailOptIn=$false}
Check ($first.Status -eq 201) 'User registration'
$token=$first.Data.token
$second=Call-Api POST '/auth/register' '' @{name='Other User';email="other-$stamp@example.test";password=$password;emailOptIn=$false}
Check ($second.Status -eq 201) 'Second independent account'
$other=$second.Data.token
$admin=Call-Api POST '/auth/login' '' @{email=$settings.MUSIC_ADMIN_EMAIL;password=$settings.MUSIC_ADMIN_PASSWORD}
Check ($admin.Status -eq 200) 'Admin login'
$adminToken=$admin.Data.token
Check ((Call-Api GET '/songs').Status -eq 401) 'Anonymous song access rejected'
Check ((Call-Api GET '/admin/songs' $token).Status -eq 403) 'Normal user cannot access admin API'
$songs=Call-Api GET '/songs' $token
Check ($songs.Status -eq 200) 'Song library retrieval'
Check ((Call-Api POST '/auth/login' '' @{email="verify-$stamp@example.test";password='incorrect-password'}).Status -eq 401) 'Wrong password rejected'
$invalid=@{name='';singer='Tester';musicDirector='Tester';releaseDate='2026-01-01';albumName='Test';genre='Ambient';visible=$true;durationSeconds=24}
Check ((Call-Api POST '/admin/songs' $adminToken $invalid).Status -eq 400) 'Blank song validation'
$song=@{name="Verification $stamp";singer='Distinctive Artist';musicDirector='Unique Director';releaseDate='2026-01-01';albumName='Special Album';genre='Ambient';visible=$true;durationSeconds=24;coverImageUrl='/assets/cover-0.svg';audioUrl='/assets/demo-0.wav'}
$created=Call-Api POST '/admin/songs' $adminToken $song
Check ($created.Status -eq 201) 'Admin creates song'
$id=$created.Data.id
Check ((Call-Api GET "/songs/$id" $token).Data.name -eq $song.name) 'Fetch single song'
foreach($term in @('distinctive','unique','special')){
    $result=Call-Api GET "/songs?keyword=$term" $token
    Check (@($result.Data|Where-Object {$_.id -eq $id}).Count -eq 1) "Search matches $term"
}
Check (@((Call-Api GET '/songs?keyword=zzzznomatch984535' $token).Data).Count -eq 0) 'No-match search returns empty list'
$playlist=Call-Api POST '/playlists' $token @{name='Verification playlist'}
Check ($playlist.Status -eq 201) 'Create playlist'
$pidValue=$playlist.Data.id
Check ((Call-Api POST "/playlists/$pidValue/songs/$id" $token).Status -eq 200) 'Add song to playlist'
$again=Call-Api POST "/playlists/$pidValue/songs/$id" $token
Check (@($again.Data.songIds).Count -eq 1) 'Playlist prevents duplicate entries'
Check ((Call-Api DELETE "/playlists/$pidValue" $other).Status -eq 404) 'Other user cannot delete playlist'
Check ((Call-Api PUT "/playlists/$pidValue" $other @{name='Stolen'}).Status -eq 404) 'Other user cannot rename playlist'
Check ((Call-Api PUT "/playlists/$pidValue" $token @{name='Renamed playlist'}).Data.name -eq 'Renamed playlist') 'Rename own playlist'
$foundNotice=$false
for($n=0;$n -lt 25;$n++){
    $notices=Call-Api GET '/notifications' $token
    $notice=@($notices.Data|Where-Object {$_.songId -eq $id})
    if($notice.Count){$foundNotice=$true;break}
    Start-Sleep -Seconds 2
}
Check $foundNotice 'New-song outbox produces in-app notification'
Check ((Call-Api PUT "/notifications/$($notice[0].id)/read" $other).Status -eq 404) 'Notification ownership enforced'
Check ((Call-Api PUT "/notifications/$($notice[0].id)/read" $token).Status -eq 200) 'Mark own notification read'
$song.visible=$false
Check ((Call-Api PUT "/admin/songs/$id" $adminToken $song).Status -eq 200) 'Admin hides song'
Check ((Call-Api GET "/songs/$id" $token).Status -eq 404) 'Hidden song direct access blocked'
Check (@((Call-Api GET "/songs?keyword=$stamp" $token).Data).Count -eq 0) 'Hidden song excluded from search'
Check ((Call-Api POST "/playlists/$pidValue/songs/$id" $token).Status -eq 404) 'Hidden song cannot be added to playlist'
Check ((Call-Api DELETE "/playlists/$pidValue/songs/$id" $token).Status -eq 200) 'Remove playlist song'
Check ((Call-Api DELETE "/playlists/$pidValue" $token).Status -eq 204) 'Delete own playlist'
Check ((Call-Api DELETE "/admin/songs/$id" $adminToken).Status -eq 204) 'Delete song'
Check ((Call-Api GET "/songs/$id" $token).Status -eq 404) 'Deleted song returns 404'
Check ((Call-Api POST '/auth/logout' $token).Status -eq 204) 'Logout succeeds'
Check ((Call-Api GET '/songs' $token).Status -eq 401) 'Logged-out JWT rejected across services'
Call-Api POST '/auth/logout' $other | Out-Null
Call-Api POST '/auth/logout' $adminToken | Out-Null
Write-Host "$script:passed integration checks passed. Temporary songs and playlists were removed. Two test accounts and their historical notifications remain."



