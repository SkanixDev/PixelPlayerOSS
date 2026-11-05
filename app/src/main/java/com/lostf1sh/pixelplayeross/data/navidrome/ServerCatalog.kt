package com.lostf1sh.pixelplayeross.data.navidrome

import com.lostf1sh.pixelplayeross.data.model.Album
import com.lostf1sh.pixelplayeross.data.model.Artist
import com.lostf1sh.pixelplayeross.data.model.Song
import java.util.concurrent.ConcurrentHashMap

data class ServerSearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList()
)

data class RemoteEntry<T>(val serverId: String, val item: T)

/**
 * Songs, albums and artists found on the server that are not in the library.
 *
 * The app opens everything by numeric id, so a remote item gets one too and is kept here to find
 * its way back to the server. Entries last as long as the process: a detail screen restored
 * after the app was killed reports the item as not found.
 */
internal class RemoteCatalog {
    private val songs = ConcurrentHashMap<Long, Song>()
    private val albums = ConcurrentHashMap<Long, RemoteEntry<Album>>()
    private val artists = ConcurrentHashMap<Long, RemoteEntry<Artist>>()

    fun song(id: Long): Song? = songs[id]

    fun album(id: Long): RemoteEntry<Album>? = albums[id]

    fun artist(id: Long): RemoteEntry<Artist>? = artists[id]

    fun putSong(id: Long, song: Song) {
        songs[id] = song
    }

    fun putAlbum(album: Album, serverId: String) {
        albums[album.id] = RemoteEntry(serverId, album)
    }

    fun putArtist(artist: Artist, serverId: String) {
        artists[artist.id] = RemoteEntry(serverId, artist)
    }
}
