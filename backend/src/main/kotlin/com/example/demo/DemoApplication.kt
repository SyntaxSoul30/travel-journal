package com.example.demo

import com.example.demo.config.AppConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    AppConfig().mongoSetup()
	runApplication<DemoApplication>(*args)
}
