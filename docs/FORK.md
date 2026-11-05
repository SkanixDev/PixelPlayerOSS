# Personal fork notes

This fork adds server-first search on top of PixelPlayerOSS, so search can reach any track
through [octo-fiesta](https://github.com/V1ck3s/octo-fiesta). The app stays a plain Subsonic
client: all downloading happens on the proxy.

```
App -> octo-fiesta -> Navidrome
           '-> streaming provider
```

## Setup

1. Run Navidrome and octo-fiesta.
2. In the app, log in to the Navidrome integration with the octo-fiesta URL (default port
   5274) instead of the Navidrome URL.
3. Check `Settings > Music Management > Optional online services > Search the server first`
   is on. It has no effect until a server is connected, and it works with plain Navidrome too.

## What changed

| Behaviour | Where |
| --- | --- |
| Song results come from the server, then local songs it did not return | `MusicRepositoryImpl.searchSongsServerFirst`, `mergeServerFirst` |
| Results get a numeric song id, as the rest of the app expects | `NavidromeRepository.searchPlayableSongs` |
| External tracks (`ext-` ids) play alone, so auto-advance never downloads a track nobody picked | `searchPlaybackQueue` |
| Up to 3 minutes for the first byte, since octo-fiesta sends nothing before the download ends | `CLOUD_FIRST_RESPONSE_TIMEOUT_MS` |
| Adding to a Navidrome playlist also writes to the server, retrying until the track is ready | `ServerPlaylistAddWorker` |

| Albums and artists come from the server too, and open like library ones | `NavidromeRepository.searchServer`, `RemoteCatalog` |
| Albums and artists play as a queue: each external track downloads when its turn comes | `getSongsForAlbum`, `getSongsForArtist` |
| Listens to tracks not in the library still reach ListenBrainz, sent by the app | `ScrobbleManager`, `RemoteCatalog` |

Playlists in search results stay local.

## Limits

- External tracks can only be added to existing Navidrome playlists. The app does not create
  server playlists.
- If a playlist addition fails for good, it only shows in the logs.
- An artist page loads at most 30 albums.
- Connect ListenBrainz in the app, not in Navidrome: both together would count listens twice,
  and octo-fiesta drops the server scrobble of a track that is not available yet.
- A listen to an external track is lost if the app restarts before the session ends.
- A remote album or artist page restored after the app was killed shows as not found.
- A played external track is not in the library until the next sync, so favorites and stats
  on it stay under its temporary id.

## Staying up to date

`origin` is this fork and `upstream` is PixelPlayerHQ/PixelPlayerOSS.

```sh
git fetch upstream
git switch main
git merge --ff-only upstream/main
git switch feat/server-first-search
git merge main
```

## Building on this machine

The project path contains a non-ASCII character. On Windows the Android Gradle Plugin rejects
it and unit tests cannot find their classes, so build from an ASCII drive letter, with JDK 21:

```sh
subst P: "C:\Users\skani\Documents\Développement\Github\PixelPlayerOSS"
cd /d P:\
gradlew.bat :app:assembleDebug -Ppixelplayer.enableAbiSplits=false
```
