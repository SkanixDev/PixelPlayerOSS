package com.lostf1sh.pixelplayeross.data.navidrome

import com.lostf1sh.pixelplayeross.data.model.Playlist
import com.lostf1sh.pixelplayeross.data.model.Song
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ServerTrackTest {

    private fun song(
        id: String,
        title: String = "Title $id",
        artist: String = "Artist",
        navidromeId: String? = null
    ) = Song.emptySong().copy(id = id, title = title, artist = artist, navidromeId = navidromeId)

    private fun serverPlaylist(serverId: String, songIds: List<String> = emptyList()) =
        Playlist(
            id = "navidrome_playlist:$serverId",
            name = serverId,
            songIds = songIds,
            source = "NAVIDROME"
        )

    @Test
    fun `only ext ids are external tracks`() {
        assertTrue(song("1", navidromeId = "ext-deezer-song-42").isExternalServerTrack)
        assertFalse(song("2", navidromeId = "a1b2c3").isExternalServerTrack)
        assertFalse(song("3").isExternalServerTrack)
    }

    @Test
    fun `server songs come first and local songs follow`() {
        val server = song("10", navidromeId = "a")
        val local = song("20")

        assertEquals(listOf(server, local), mergeServerFirst(listOf(server), listOf(local)))
    }

    @Test
    fun `local copy of a server track is dropped`() {
        val server = song("10", navidromeId = "a")
        val sameTrack = song("11", navidromeId = "a", title = "Other title")

        assertEquals(listOf(server), mergeServerFirst(listOf(server), listOf(sameTrack)))
    }

    @Test
    fun `local file with the same title and artist is dropped`() {
        val server = song("10", title = "Hello", artist = "Adele", navidromeId = "ext-deezer-song-1")
        val localFile = song("20", title = " hello ", artist = "ADELE")

        assertEquals(listOf(server), mergeServerFirst(listOf(server), listOf(localFile)))
    }

    @Test
    fun `no server results leaves local songs untouched`() {
        val local = listOf(song("20"), song("21"))

        assertEquals(local, mergeServerFirst(emptyList(), local))
    }

    @Test
    fun `external result plays alone`() {
        val external = song("1", navidromeId = "ext-deezer-song-1")
        val library = song("2", navidromeId = "a")

        assertEquals(listOf(external), searchPlaybackQueue(external, listOf(library, external)))
    }

    @Test
    fun `library result queues the other results but no external one`() {
        val external = song("1", navidromeId = "ext-deezer-song-1")
        val first = song("2", navidromeId = "a")
        val second = song("3")

        assertEquals(
            listOf(first, second),
            searchPlaybackQueue(first, listOf(first, external, second))
        )
    }

    @Test
    fun `result missing from the list plays alone`() {
        val song = song("1")

        assertEquals(listOf(song), searchPlaybackQueue(song, listOf(song("2"))))
    }

    @Test
    fun `server playlist id comes from real mirrored playlists only`() {
        assertEquals("abc", NavidromeRepository.serverPlaylistIdOf("navidrome_playlist:abc"))
        assertEquals(null, NavidromeRepository.serverPlaylistIdOf("some-local-id"))
        assertEquals(
            null,
            NavidromeRepository.serverPlaylistIdOf(
                "navidrome_playlist:${NavidromeRepository.LIBRARY_PLAYLIST_ID}"
            )
        )
    }

    @Test
    @DisplayName("Only new server songs in server playlists need a server addition")
    fun serverPlaylistAdditions() {
        val external = song("1", navidromeId = "ext-deezer-song-1")
        val alreadyInPlaylist = song("2", navidromeId = "a")
        val localFile = song("3")

        val additions = serverPlaylistAdditions(
            songs = listOf(external, alreadyInPlaylist, localFile),
            playlists = listOf(
                serverPlaylist("p1", songIds = listOf("2")),
                Playlist(id = "local", name = "Local", songIds = emptyList())
            )
        )

        assertEquals(listOf(ServerPlaylistAddition("p1", "ext-deezer-song-1")), additions)
    }
}
