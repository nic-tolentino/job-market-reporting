package com.techmarket

import com.techmarket.config.ApifyProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication(
        exclude =
                [com.google.cloud.spring.autoconfigure.bigquery.GcpBigQueryAutoConfiguration::class]
)
@EnableConfigurationProperties(ApifyProperties::class)
@EnableAsync
@EnableCaching
class TechMarketApplication

fun main(args: Array<String>) {
    runApplication<TechMarketApplication>(*args)
}
