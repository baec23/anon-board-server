package com.baec23.anonboard.services

import com.baec23.anonboard.controllers.CreatePostRequestForm
import com.baec23.anonboard.model.Post
import com.baec23.anonboard.repositories.PostRepository
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
class PostService constructor(
    private val postRepository: PostRepository
) {
    private val sseEmitters: MutableList<SseEmitter> = mutableListOf()
    private val emittersToRemove = mutableListOf<SseEmitter>()

    fun registerSseEmitter(): SseEmitter {
        val emitter = SseEmitter(600000L)   // 10 minutes
        runBlocking {
            val event = SseEmitter.event().data(postRepository.findAll())
            emitter.send(event)
            sseEmitters.add(emitter)
        }
        emitter.onTimeout { runBlocking { emittersToRemove.add(emitter) } }

        return emitter
    }

    fun savePost(requestForm: CreatePostRequestForm): Post {

        val toSave = Post(
            id = null,
            userDisplayName = requestForm.userDisplayName,
            message = requestForm.message,
            createdTimestamp = System.currentTimeMillis(),
            parentId = requestForm.parentId,
            childIds = listOf(),
        )
        val savedPost = postRepository.save(toSave)
        if (requestForm.parentId != null) {
            val result = postRepository.findById(requestForm.parentId)
            if (result.isPresent) {
                val parentPost = result.get()
                val mutableChildIds = parentPost.childIds.toMutableList()
                savedPost.id?.let { mutableChildIds.add(it) }
                postRepository.save(parentPost.copy(childIds = mutableChildIds.toList()))
            }
        }
        runBlocking { emitUpdates() }
        return savedPost
    }

    private suspend fun emitUpdates() {
        emittersToRemove.forEach {
            try {
                it.complete()
            } catch (_: Exception) {
            }
            sseEmitters.removeAll(emittersToRemove)
        }
        emittersToRemove.clear()
        val event = SseEmitter.event().data(postRepository.findAll())
        sseEmitters.forEach { emitter ->
            try {
                emitter.send(event)
            } catch (e: Exception) {
                emittersToRemove.add(emitter)
            }
        }
        emittersToRemove.forEach {
            try {
                it.complete()
            } catch (_: Exception) {
            }
            sseEmitters.removeAll(emittersToRemove)
        }
        emittersToRemove.clear()
    }
}