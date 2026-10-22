package com.example.demo.controller

import com.example.demo.model.internal.CaptionRequest
import com.example.demo.model.internal.CaptionResponse
import com.example.demo.model.internal.GetImageAndMetadataRequest
import com.example.demo.model.internal.GetImageAndMetadataResponse
import com.example.demo.service.CaptionAndSummaryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class CaptionAndSummaryController @Autowired constructor(
    val captionAndSummaryService: CaptionAndSummaryService
) {

    @PostMapping("/getAutoCaptions")
    fun generateCaptions(@RequestBody request: CaptionRequest): CaptionResponse {
        return captionAndSummaryService.getSummaryAndCaptionForImage(request)
    }

    @PostMapping("/getImageAndMetadata")
    fun getImageAndMetadata(@RequestBody request: GetImageAndMetadataRequest): GetImageAndMetadataResponse {
        return captionAndSummaryService.getImageAndMetadata(request)
    }

}