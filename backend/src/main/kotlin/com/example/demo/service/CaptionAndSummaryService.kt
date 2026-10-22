package com.example.demo.service

import com.example.demo.config.AppConfig
import com.example.demo.entity.CaptionsAndSummaries
import com.example.demo.entity.ImageAndMetadata
import com.example.demo.entity.Metadata
import com.example.demo.model.external.ContentPart
import com.example.demo.model.external.GroqMessage
import com.example.demo.model.external.GroqRequest
import com.example.demo.model.external.GroqResponse
import com.example.demo.model.external.ImageUrl
import com.example.demo.model.external.ResponseFormat
import com.example.demo.model.internal.CaptionRequest
import com.example.demo.model.internal.CaptionResponse
import com.example.demo.model.internal.GetImageAndMetadataRequest
import com.example.demo.model.internal.GetImageAndMetadataResponse
import com.example.demo.model.internal.ImageDetails
import com.example.demo.repository.ImageAndMetadataRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URI
import org.springframework.http.MediaType

@Service
class CaptionAndSummaryService @Autowired constructor(
    val restClient: RestClient,
    val objectMapper: ObjectMapper,
    val appConfig: AppConfig,
    val repository: ImageAndMetadataRepository
) {

    private val DEFAULT_PROMPT = """
        First, generate a quirky, concise and suitable caption suggestion for this image.
        Second, generate a short, most accurate and descriptive summary suggestion for this image's content.
        Respond ONLY with a JSON object in the format:
        {"caption": "The best caption chosen.", "summary": "The best summary chosen."}
        Do not include any other text, explanations, or markdown.
    """.trimIndent()

    fun getSummaryAndCaptionForImage(request: CaptionRequest): CaptionResponse {
        val promptToUse = DEFAULT_PROMPT
        val imageUrl = "data:${request.imageType};base64,${request.imageContentB64}"
        val groqRequest = GroqRequest(
            model = "meta-llama/llama-4-scout-17b-16e-instruct", messages = listOf(
                GroqMessage(
                    role = "user", content = listOf(
                        ContentPart(type = "text", text = promptToUse),
                        ContentPart(type = "image_url", imageUrl = ImageUrl(url = imageUrl))
                    )
                )
            ), maxTokens = 300, responseFormat = ResponseFormat(type = "json_object")
        )

        val loadEnvFile = appConfig.loadEnvFile()
        loadEnvFile.forEach { (key, value) ->
            System.setProperty(key, value)
        }
        val groqApiKey: String = System.getProperty("GROK_API_KEY") ?: System.getenv("GROK_API_KEY")
        ?: throw IllegalStateException("GROK_API_KEY not found. Check .env file.")
        val groqApiUrl: String = System.getProperty("GROK_API_URL") ?: System.getenv("GROK_API_URL")
        ?: throw IllegalStateException("GROK_API_URL not found. Check .env file.")

        try {
            val groqResponse: GroqResponse? =
                restClient.post().uri(URI(groqApiUrl)).header("Authorization", "Bearer $groqApiKey")
                    .contentType(MediaType.APPLICATION_JSON).body(groqRequest).retrieve().body(GroqResponse::class.java)

            if (groqResponse == null || groqResponse.choices.isNullOrEmpty()) {
                throw RuntimeException("Groq returned no choices or an empty response body.")
            }
            val jsonContentString = groqResponse.choices[0].message.content
            val captionJson: CaptionResponse = objectMapper.readValue(jsonContentString)

            val imageAndMetadata = ImageAndMetadata(
                imageContentB64 = request.imageContentB64,
                metadata = Metadata(
                    fileName = request.fileName,
                    location = request.location,
                    imgType = request.imageType,
                    capturedTs = request.capturedTs

                ),
                captionsAndSummaries = CaptionsAndSummaries(
                    caption = captionJson.caption,
                    summary = captionJson.summary
                )
            )
            repository.save<ImageAndMetadata>(imageAndMetadata)
            return CaptionResponse(
                caption = captionJson.caption, summary = captionJson.summary
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to get caption from Groq: ${e.message}", e)
        }
    }

    fun getImageAndMetadata(request: GetImageAndMetadataRequest): GetImageAndMetadataResponse {
        val imageAndMetadata = repository.findByMetadataFileName(request.fileName)
            ?: throw Exception("No such file in the repository")
        val imageDetails = ImageDetails(
            imageContentB64 = imageAndMetadata.imageContentB64,
            metadata = Metadata(
                fileName = imageAndMetadata.metadata.fileName,
                location = imageAndMetadata.metadata.location,
                imgType = imageAndMetadata.metadata.imgType,
                capturedTs = imageAndMetadata.metadata.capturedTs
            ),
            captionsAndSummaries = CaptionsAndSummaries(
                caption = imageAndMetadata.captionsAndSummaries.caption,
                summary = imageAndMetadata.captionsAndSummaries.summary
            )
        )
        return GetImageAndMetadataResponse(imageDetails = imageDetails)
    }


}