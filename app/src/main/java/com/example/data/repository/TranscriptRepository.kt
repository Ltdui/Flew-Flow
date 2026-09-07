package com.example.data.repository

import com.example.data.local.TranscriptDao
import com.example.data.model.TranscriptEntity
import kotlinx.coroutines.flow.Flow

class TranscriptRepository(private val transcriptDao: TranscriptDao) {
    val allTranscripts: Flow<List<TranscriptEntity>> = transcriptDao.getAllTranscripts()

    suspend fun getById(id: Long): TranscriptEntity? = transcriptDao.getTranscriptById(id)

    suspend fun save(transcript: TranscriptEntity): Long = transcriptDao.insertTranscript(transcript)

    suspend fun update(transcript: TranscriptEntity) = transcriptDao.updateTranscript(transcript)

    suspend fun delete(transcript: TranscriptEntity) = transcriptDao.deleteTranscript(transcript)

    suspend fun deleteById(id: Long) = transcriptDao.deleteTranscriptById(id)
}
