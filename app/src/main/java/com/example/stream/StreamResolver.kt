package com.example.stream

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ResolvedTrackInfo(
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val isRemoteStream: Boolean,
    val bpm: Int = 124,
    val keySignature: String = "C min"
)

object StreamResolver {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val YOUTUBE_PATTERN = Pattern.compile(
        """(?:https?://)?(?:www\.|m\.|music\.)?(?:youtube\.com/(?:watch\?(?:.*&)?v=|shorts/|embed/)|youtu\.be/)([\w-]{11})""",
        Pattern.CASE_INSENSITIVE
    )

    fun isYoutubeUrl(input: String): Boolean {
        return YOUTUBE_PATTERN.matcher(input.trim()).find()
    }

    fun extractYoutubeVideoId(input: String): String? {
        val matcher = YOUTUBE_PATTERN.matcher(input.trim())
        return if (matcher.find()) matcher.group(1) else null
    }

    suspend fun resolveStream(context: Context, inputUrlOrUri: String): Result<ResolvedTrackInfo> =
        withContext(Dispatchers.IO) {
            try {
                val trimmed = inputUrlOrUri.trim()

                // Check if YouTube URL or contains YouTube ID
                val ytId = extractYoutubeVideoId(trimmed)
                if (ytId != null) {
                    return@withContext resolveYoutubeTrack(ytId, trimmed)
                }

                // Check if local file / content URI
                if (trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
                    return@withContext resolveLocalAudio(context, Uri.parse(trimmed))
                }

                // Standard Web Audio Stream (direct MP3, AAC, FLAC, HLS, Radio Stream)
                val guessedTitle = trimmed.substringAfterLast("/").substringBefore("?").ifBlank { "Live Web Stream" }
                val cleanTitle = guessedTitle.replace("-", " ").replace("_", " ")
                    .replace(".mp3", "", ignoreCase = true)
                    .replace(".m4a", "", ignoreCase = true)
                    .replace(".aac", "", ignoreCase = true)
                    .replace(".flac", "", ignoreCase = true)

                Result.success(
                    ResolvedTrackInfo(
                        uri = trimmed,
                        title = cleanTitle,
                        artist = "Online Stream / Audiophile Web Feed",
                        durationMs = 0L,
                        thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400",
                        isRemoteStream = true,
                        bpm = 120,
                        keySignature = "A min"
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun resolveYoutubeTrack(videoId: String, originalUrl: String): Result<ResolvedTrackInfo> {
        var title = "YouTube Audio Track"
        var uploader = "YouTube Stream"
        var thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

        try {
            // Official YouTube oEmbed provides instant, uncached title, author and thumbnail
            val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        title = json.optString("title", title)
                        uploader = json.optString("author_name", uploader)
                        val thumb = json.optString("thumbnail_url")
                        if (thumb.isNotBlank()) {
                            thumbnailUrl = thumb
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Use defaults if network oembed is slow
            title = "YouTube Audio: $videoId"
        }

        return Result.success(
            ResolvedTrackInfo(
                uri = "youtube:$videoId",
                title = title,
                artist = uploader,
                durationMs = 0L,
                thumbnailUrl = thumbnailUrl,
                isRemoteStream = true,
                bpm = 126,
                keySignature = "G min"
            )
        )
    }

    private fun resolveLocalAudio(context: Context, uri: Uri): Result<ResolvedTrackInfo> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: uri.lastPathSegment ?: "Local Audio Track"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "Local Media File"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            Result.success(
                ResolvedTrackInfo(
                    uri = uri.toString(),
                    title = title,
                    artist = artist,
                    durationMs = durationMs,
                    thumbnailUrl = null,
                    isRemoteStream = false,
                    bpm = 120,
                    keySignature = "F maj"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    suspend fun searchYoutube(query: String): Result<List<ResolvedTrackInfo>> = withContext(Dispatchers.IO) {
        try {
            val request = com.yausername.youtubedl_android.YoutubeDLRequest("ytsearch10:$query")
            request.addOption("--dump-json")
            
            val response = com.yausername.youtubedl_android.YoutubeDL.getInstance().execute(request, null, null)
            val jsonOut = response.out
            
            val tracks = mutableListOf<ResolvedTrackInfo>()
            jsonOut.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    try {
                        val json = JSONObject(line)
                        val id = json.optString("id")
                        if (id.isNotBlank()) {
                            tracks.add(ResolvedTrackInfo(
                                uri = "youtube:$id",
                                title = json.optString("title", "Unknown Title"),
                                artist = json.optString("uploader", "Unknown Artist"),
                                durationMs = json.optLong("duration", 0L) * 1000L,
                                thumbnailUrl = json.optString("thumbnail", "https://img.youtube.com/vi/$id/hqdefault.jpg"),
                                isRemoteStream = true,
                                bpm = 124,
                                keySignature = "A min"
                            ))
                        }
                    } catch (_: Exception) {}
                }
            }
            Result.success(tracks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
