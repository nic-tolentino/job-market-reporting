package com.jobmarket.app

import com.jobmarket.app.config.ApifyProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication(
        exclude =
                [com.google.cloud.spring.autoconfigure.bigquery.GcpBigQueryAutoConfiguration::class]
)
@EnableConfigurationProperties(ApifyProperties::class)
@EnableAsync
class JobMarketApplication

fun main(args: Array<String>) {
    runApplication<JobMarketApplication>(*args)
}
