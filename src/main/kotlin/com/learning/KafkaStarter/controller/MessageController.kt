package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.service.ProducerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST controller for handling message operations
 */
@RestController
@RequestMapping("/api/messages")
class MessageController(
    private val producerService: ProducerService
) {

    @PostMapping("/send")
    fun sendMessage(@RequestBody request: String) =
        producerService.sendMessage(request)
}