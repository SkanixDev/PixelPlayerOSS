package com.lostf1sh.pixelplayeross.data.navidrome

import com.lostf1sh.pixelplayeross.data.model.Album
import com.lostf1sh.pixelplayeross.data.model.Artist
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ServerCatalogTest {

    private fun album(id: Long, title: String, artist: String = "Artist") =
        Album(id, title, artist, 2026, 0L, null, 10)

    private fun artist(id: Long, name: String) = Artist(id, name, 3)

    @Test
    fun `catalog finds an album and its server id by numeric id`() {
        val catalog = RemoteCatalog()
        val album = album(-5L, "Album")

        catalog.putAlbum(album, "ext-deezer-album-1")

        assertEquals(RemoteEntry("ext-deezer-album-1", album), catalog.album(-5L))
        assertNull(catalog.album(-6L))
    }

    @Test
    fun `catalog finds an artist and its server id by numeric id`() {
        val catalog = RemoteCatalog()
        val artist = artist(-5L, "Artist")

        catalog.putArtist(artist, "ext-deezer-artist-1")

        assertEquals(RemoteEntry("ext-deezer-artist-1", artist), catalog.artist(-5L))
    }

    @Test
    fun `server albums come first and drop local duplicates`() {
        val server = album(-1L, "Discovery", "Daft Punk")
        val sameAlbum = album(2L, " discovery ", "DAFT PUNK")
        val other = album(3L, "Homework", "Daft Punk")

        assertEquals(
            listOf(server, other),
            mergeServerFirstAlbums(listOf(server), listOf(sameAlbum, other))
        )
    }

    @Test
    fun `server artists come first and drop local duplicates`() {
        val server = artist(-1L, "Daft Punk")
        val sameArtist = artist(2L, "daft punk ")
        val other = artist(3L, "Justice")

        assertEquals(
            listOf(server, other),
            mergeServerFirstArtists(listOf(server), listOf(sameArtist, other))
        )
    }
}
