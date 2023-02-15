package com.baec23.anonboard.controllers

import com.baec23.anonboard.model.Post
import com.baec23.anonboard.services.PostService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/anon-board-api/v1/post")
class PostController constructor(
    private val postService: PostService
){
    @GetMapping
    fun getAllPosts(): ResponseEntity<SseEmitter> {
        println("getAllPosts")
        val emitter = postService.registerSseEmitter()
        return ResponseEntity.ok(emitter)
    }

//    @GetMapping("/{post-id}")
//    suspend fun getChildPosts(@PathVariable("post-id") postId: String): ResponseEntity<List<Post>>{
//
//
//    }

    @PostMapping
    fun createPost(@RequestBody createPostRequestForm: CreatePostRequestForm): ResponseEntity<Post>{
        val savedPost = postService.savePost(createPostRequestForm)
        println("createPost savedPostId: " + savedPost.id)
        return ResponseEntity.ok(savedPost)
    }
}

data class CreatePostRequestForm(
    val userDisplayName: String,
    val message: String,
    val parentId: String?
)