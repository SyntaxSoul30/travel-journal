package com.example.demo.repository

import com.example.demo.entity.ImageAndMetadata
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ImageAndMetadataRepository : MongoRepository<ImageAndMetadata, String> {
    fun findByMetadataFileName(fileName: String): ImageAndMetadata?
}