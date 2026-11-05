package com.lostf1sh.pixelplayeross.data.listenbrainz

import com.lostf1sh.pixelplayeross.data.database.ListenBrainzSource
import com.lostf1sh.pixelplayeross.data.model.Song
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ListenTrackTest {

    @Test
    fun `server song not in the library becomes a Navidrome listen`() {
        val song = Song.emptySong().copy(
            id = "-9",
            title = "One More Time",
            artist = "Daft Punk",
            album = "Discovery",
            duration = 320_000L,
            navidromeId = "ext-deezer-song-1"
        )

        assertEquals(
            ListenTrack(
                title = "One More Time",
                artistName = "Daft Punk",
                albumName = "Discovery",
                duration = 320_000L,
                mbRecordingId = null,
                source = ListenBrainzSource.NAVIDROME
            ),
            song.toListenTrack()
        )
    }
}
