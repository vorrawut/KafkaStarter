package com.learning.KafkaStarter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KafkaStarterApplication

fun main(args: Array<String>) {
	runApplication<KafkaStarterApplication>(*args)
}
