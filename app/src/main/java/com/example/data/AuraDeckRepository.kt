package com.example.data

import com.example.data.dao.AuraDeckDao
import com.example.data.model.DspPresetEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.TrackEntity
import kotlinx.coroutines.flow.Flow

class AuraDeckRepository(private val dao: AuraDeckDao) {

    val allTracks: Flow<List<TrackEntity>> = dao.getAllTracks()
    val favoriteTracks: Flow<List<TrackEntity>> = dao.getFavoriteTracks()
    val allPlaylists: Flow<List<PlaylistEntity>> = dao.getAllPlaylists()
    val dspPresets: Flow<List<DspPresetEntity>> = dao.getAllDspPresets()

    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>> =
        dao.getTracksForPlaylist(playlistId)

    suspend fun getTrackById(id: Long): TrackEntity? = dao.getTrackById(id)

    suspend fun getTrackByUri(uri: String): TrackEntity? = dao.getTrackByUri(uri)

    suspend fun insertTrack(track: TrackEntity): Long = dao.insertTrack(track)

    suspend fun updateTrack(track: TrackEntity) = dao.updateTrack(track)

    suspend fun deleteTrack(track: TrackEntity) = dao.deleteTrack(track)

    suspend fun insertPlaylist(playlist: PlaylistEntity): Long = dao.insertPlaylist(playlist)

    suspend fun deletePlaylist(playlist: PlaylistEntity) = dao.deletePlaylist(playlist)

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long, orderIndex: Int) =
        dao.insertPlaylistCrossRef(PlaylistTrackCrossRef(playlistId, trackId, orderIndex))

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) =
        dao.removeTrackFromPlaylist(playlistId, trackId)

    suspend fun insertDspPreset(preset: DspPresetEntity): Long = dao.insertDspPreset(preset)

    suspend fun deleteDspPreset(preset: DspPresetEntity) = dao.deleteDspPreset(preset)
}
