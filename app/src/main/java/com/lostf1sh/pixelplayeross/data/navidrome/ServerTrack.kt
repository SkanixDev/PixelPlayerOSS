package com.lostf1sh.pixelplayeross.data.navidrome

import com.lostf1sh.pixelplayeross.data.model.Playlist
import com.lostf1sh.pixelplayeross.data.model.Song

/**
 * Prefix octo-fiesta gives to tracks that come from a streaming provider and are not in the
 * Navidrome library yet. Such a track is downloaded by the proxy the first time it is streamed,
 * and gets a regular Navidrome id once the library scan picks it up.
 */
private const val EXTERNAL_TRACK_ID_PREFIX = "ext-"

private const val NAVIDROME_PLAYLIST_SOURCE = "NAVIDROME"

fun isExternalServerTrackId(navidromeId: String): Boolean =
    navidromeId.startsWith(EXTERNAL_TRACK_ID_PREFIX)

val Song.isExternalServerTrack: Boolean
    get() = navidromeId?.let(::isExternalServerTrackId) == true

val Playlist.isWritableServerPlaylist: Boolean
    get() = source == NAVIDROME_PLAYLIST_SOURCE && NavidromeRepository.serverPlaylistIdOf(id) != null

/**
 * Puts the server results first, then the local songs the server did not return. A local song
 * is a duplicate when it is the same server track, or has the same title and artist.
 */
fun mergeServerFirst(server: List<Song>, local: List<Song>): List<Song> {
    val serverSongIds = server.mapTo(HashSet()) { it.id }
    val serverNavidromeIds = server.mapNotNullTo(HashSet()) { it.navidromeId }
    val serverTitles = server.mapTo(HashSet()) { it.titleKey() }

    return server + local.filterNot { song ->
        song.id in serverSongIds ||
            song.navidromeId in serverNavidromeIds ||
            song.titleKey() in serverTitles
    }
}

private fun Song.titleKey(): String = "${title.trim().lowercase()}|${artist.trim().lowercase()}"

/**
 * The queue to play a search result in. Auto-advancing into an external result would make the
 * proxy download a song nobody picked, so external results only ever play on their own and
 * stay out of the queue of other results.
 */
fun searchPlaybackQueue(song: Song, results: List<Song>): List<Song> {
    if (song.isExternalServerTrack) return listOf(song)
    val playableResults = results.filterNot { it.isExternalServerTrack }
    return if (playableResults.any { it.id == song.id }) playableResults else listOf(song)
}

data class ServerPlaylistAddition(val playlistId: String, val songId: String)

/**
 * The server-side additions needed to add [songs] to [playlists]. Local playlists, local songs
 * and songs the playlist already holds need none.
 */
fun serverPlaylistAdditions(
    songs: List<Song>,
    playlists: List<Playlist>
): List<ServerPlaylistAddition> = playlists
    .filter { it.isWritableServerPlaylist }
    .flatMap { playlist ->
        val serverPlaylistId = NavidromeRepository.serverPlaylistIdOf(playlist.id)
            ?: return@flatMap emptyList()
        songs
            .filter { song -> song.id !in playlist.songIds }
            .mapNotNull { song -> song.navidromeId }
            .map { songId -> ServerPlaylistAddition(serverPlaylistId, songId) }
    }
