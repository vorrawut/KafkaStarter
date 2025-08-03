package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.model.PriceRequest
import com.learning.KafkaStarter.model.ValidationRequest
import com.learning.KafkaStarter.service.RequestReplyService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/request-reply")
class RequestReplyController {
    
    @Autowired
    private lateinit var requestReplyService: RequestReplyService
    
    @PostMapping("/price/request")
    fun requestPrice(@RequestBody request: PriceRequest): ResponseEntity<Map<String, Any>> {
        return try {
            // TODO: Send price request and return response
            // HINT: Use requestReplyService to send request and wait for reply
            TODO("Send price request")
        } catch (e: Exception) {
            // TODO: Handle price request error
            TODO("Handle price request error")
        }
    }
    
    @PostMapping("/validation/request")
    fun requestValidation(@RequestBody request: ValidationRequest): ResponseEntity<Map<String, Any>> {
        return try {
            // TODO: Send validation request and return response
            // HINT: Use requestReplyService to send request and wait for reply
            TODO("Send validation request")
        } catch (e: Exception) {
            // TODO: Handle validation request error
            TODO("Handle validation request error")
        }
    }
    
    @PostMapping("/async-request")
    fun sendAsyncRequest(
        @RequestParam topic: String,
        @RequestParam replyTopic: String,
        @RequestBody requestData: Map<String, Any>
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // TODO: Send async request without waiting for reply
            // HINT: Use requestReplyService to send async request
            TODO("Send async request")
        } catch (e: Exception) {
            // TODO: Handle async request error
            TODO("Handle async request error")
        }
    }
    
    @GetMapping("/stats")
    fun getRequestReplyStats(): ResponseEntity<Map<String, Any>> {
        return try {
            // TODO: Get request-reply processing statistics
            // HINT: Return metrics about requests, responses, and timeouts
            TODO("Get request-reply statistics")
        } catch (e: Exception) {
            // TODO: Handle stats retrieval error
            TODO("Handle stats error")
        }
    }
}