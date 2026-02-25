package com.jobmarket.app

import com.jobmarket.app.config.ApifyProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ApifyProperties::class)
class JobMarketApplication

fun main(args: Array<String>) {
    runApplication<JobMarketApplication>(*args)
}
