package com.techmarket

import com.techmarket.config.ApifyProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableConfigurationProperties(ApifyProperties::class)
@EnableCaching
class TechMarketApplication

fun main(args: Array<String>) {
    runApplication<TechMarketApplication>(*args)
}
