package com.example.demo.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.ServerApi
import com.mongodb.ServerApiVersion
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.springframework.context.annotation.Bean
import java.io.File

import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class AppConfig {

    @Bean
    fun restClient(): RestClient {
        return RestClient.create()
    }

    fun loadEnvFile(fileName: String = ".env"): Map<String, String> {
        val envMap = mutableMapOf<String, String>()
        val envFile = File(fileName)
        if (!envFile.exists()) {
            System.err.println("Configuration file '$fileName' not found in project root. Trying system environment variables.")
            return emptyMap()
        }
        envFile.readLines().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                return@forEach
            }
            val parts = trimmedLine.split('=', limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim().trim { it == '"' || it == '\'' }
                envMap[key] = value
            }
        }
        return envMap
    }

    fun mongoSetup(): Boolean {
        val envMap = loadEnvFile()

        envMap.forEach { (key, value) ->
            System.setProperty(key, value)
        }

        val connectionString = System.getProperty("MONGO_CONNECTION_STRING") ?: System.getenv("MONGO_CONNECTION_STRING")
            ?: throw IllegalStateException("MONGO_CONNECTION_STRING not found. Check .env file.")

        return try {
            val serverApi = ServerApi.builder().version(ServerApiVersion.V1).build()
            val mongoClientSettings =
                MongoClientSettings.builder().applyConnectionString(ConnectionString(connectionString))
                    .serverApi(serverApi).build()

            MongoClient.create(mongoClientSettings).use { mongoClient ->
                val database = mongoClient.getDatabase("admin")
                runBlocking {
                    database.runCommand(Document("ping", 1))
                }
                println("You successfully connected to MongoDB!")
            }
            true
        } catch (e: Exception) {
            System.err.println("Failed to connect to MongoDB Atlas during pre-flight check.")
            e.printStackTrace()
            false
        }
    }
}