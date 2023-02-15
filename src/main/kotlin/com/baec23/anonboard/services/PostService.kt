package com.baec23.anonboard.services

import com.baec23.anonboard.controllers.CreatePostRequestForm
import com.baec23.anonboard.model.Post
import com.baec23.anonboard.repositories.PostRepository
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
class PostService constructor(
    private val postRepository: PostRepository
) {
    private val sseEmitters: MutableList<SseEmitter> = mutableListOf()

    fun registerSseEmitter(): SseEmitter {
        val emitter = SseEmitter(0L)
        sseEmitters.add(emitter)
        val event = SseEmitter.event().data(postRepository.findAll())
        emitter.send(event)
        return emitter
    }

    fun savePost(requestForm: CreatePostRequestForm): Post {
        var nestingLevel = 0;
        if(requestForm.parentId != null){
            val result = postRepository.findById(requestForm.parentId)
            if(result.isPresent){
                val parentPost = result.get()
                nestingLevel = parentPost.nestingLevel+1
            }
        }

        val toSave = Post(
            id = null,
            userDisplayName = requestForm.userDisplayName,
            message = requestForm.message,
            createdTimestamp = System.currentTimeMillis(),
            parentId = requestForm.parentId,
            childIds = listOf(),
            nestingLevel = nestingLevel
        )
        val savedPost = postRepository.save(toSave)
        if(requestForm.parentId != null){
            val result = postRepository.findById(requestForm.parentId)
            if(result.isPresent){
                val parentPost = result.get()
                val mutableChildIds = parentPost.childIds.toMutableList()
                savedPost.id?.let { mutableChildIds.add(it) }
                postRepository.save(parentPost.copy(childIds = mutableChildIds.toList()))
            }
        }
        emitUpdates()
        return savedPost
    }

    private fun emitUpdates() {
        val event = SseEmitter.event().data(postRepository.findAll())
        val emittersToRemove = mutableListOf<SseEmitter>()
        sseEmitters.forEach { emitter ->
            try {
                emitter.send(event)
            } catch (e: Exception) {
                emitter.complete()
                emittersToRemove.add(emitter)
            }
        }
        sseEmitters.removeAll(emittersToRemove)
    }
}