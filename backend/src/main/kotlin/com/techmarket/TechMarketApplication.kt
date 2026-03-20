package com.techmarket

import com.techmarket.config.ApifyProperties
import com.techmarket.config.SmartRecruitersProperties
import com.techmarket.config.WorkableProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableConfigurationProperties(
    ApifyProperties::class,
    SmartRecruitersProperties::class,
    WorkableProperties::class,
)
@EnableCaching
@EnableScheduling
class TechMarketApplication

fun main(args: Array<String>) {
    runApplication<TechMarketApplication>(*args)
}
