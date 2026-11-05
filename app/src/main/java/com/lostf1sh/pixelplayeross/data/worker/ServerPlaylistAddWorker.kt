package com.lostf1sh.pixelplayeross.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lostf1sh.pixelplayeross.data.navidrome.NavidromeRepository
import com.lostf1sh.pixelplayeross.data.navidrome.ServerPlaylistAddition
import com.lostf1sh.pixelplayeross.data.navidrome.isExternalServerTrackId
import com.lostf1sh.pixelplayeross.data.network.navidrome.SubsonicApiException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Adds one song to a server playlist.
 *
 * An external octo-fiesta track can only join a playlist once the proxy has downloaded it and
 * Navidrome has scanned it, which takes a while after the first stream request. Until then the
 * proxy answers "not found", so this retries in the background instead of failing.
 */
@HiltWorker
class ServerPlaylistAddWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val navidromeRepository: NavidromeRepository,
    baseOkHttpClient: OkHttpClient
) : CoroutineWorker(appContext, workerParams) {

    private val downloadClient = baseOkHttpClient.newBuilder()
        .readTimeout(DOWNLOAD_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val playlistId = inputData.getString(KEY_PLAYLIST_ID)
            ?: return@withContext Result.failure()
        val songId = inputData.getString(KEY_SONG_ID)
            ?: return@withContext Result.failure()

        if (runAttemptCount == 0 && isExternalServerTrackId(songId)) {
            requestServerDownload(songId)
        }

        navidromeRepository.addSongToServerPlaylist(playlistId, songId).fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                if (shouldRetryServerPlaylistAdd(error, songId, runAttemptCount)) {
                    Result.retry()
                } else {
                    Timber.e(error, "$TAG: Could not add a song to playlist $playlistId")
                    Result.failure()
                }
            }
        )
    }

    /**
     * Makes the proxy download a track the user added without playing it. The proxy only
     * answers once the file is ready, so the response itself is never read.
     */
    private fun requestServerDownload(songId: String) {
        try {
            val request = Request.Builder()
                .url(navidromeRepository.getStreamUrl(songId))
                .build()
            downloadClient.newCall(request).execute().close()
        } catch (e: IOException) {
            Timber.w(e, "$TAG: Download request failed, retrying the playlist add anyway")
        }
    }

    companion object {
        private const val TAG = "ServerPlaylistAdd"
        private const val KEY_PLAYLIST_ID = "playlist_id"
        private const val KEY_SONG_ID = "song_id"
        private const val DOWNLOAD_TIMEOUT_MINUTES = 5L
        private const val RETRY_DELAY_SECONDS = 20L
        private const val SUBSONIC_ERROR_NOT_FOUND = 70

        internal const val MAX_ATTEMPTS = 8

        internal fun shouldRetryServerPlaylistAdd(
            error: Throwable,
            songId: String,
            attempt: Int
        ): Boolean {
            if (attempt + 1 >= MAX_ATTEMPTS) return false
            return when (error) {
                is IOException -> true
                is SubsonicApiException ->
                    error.code == SUBSONIC_ERROR_NOT_FOUND && isExternalServerTrackId(songId)
                else -> false
            }
        }

        internal fun workName(addition: ServerPlaylistAddition): String =
            "server_playlist_add_${addition.playlistId}_${addition.songId}"

        internal fun request(addition: ServerPlaylistAddition) =
            OneTimeWorkRequestBuilder<ServerPlaylistAddWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_DELAY_SECONDS, TimeUnit.SECONDS)
                .setInputData(
                    workDataOf(
                        KEY_PLAYLIST_ID to addition.playlistId,
                        KEY_SONG_ID to addition.songId
                    )
                )
                .build()
    }
}

@Singleton
class ServerPlaylistQueue @Inject constructor(
    private val workManager: WorkManager
) {
    fun enqueue(additions: List<ServerPlaylistAddition>) {
        additions.forEach { addition ->
            workManager.enqueueUniqueWork(
                ServerPlaylistAddWorker.workName(addition),
                ExistingWorkPolicy.KEEP,
                ServerPlaylistAddWorker.request(addition)
            )
        }
    }
}
