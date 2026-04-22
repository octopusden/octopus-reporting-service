package org.octopusden.octopus.reportingservice.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/hello")
@RestController
class HelloController {
    @GetMapping
    fun hello(): String = "Hello World"
}