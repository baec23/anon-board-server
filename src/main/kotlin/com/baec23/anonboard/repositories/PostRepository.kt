package com.baec23.anonboard.repositories

import com.baec23.anonboard.model.Post
import org.springframework.data.mongodb.repository.MongoRepository

interface PostRepository: MongoRepository<Post, String> {
}