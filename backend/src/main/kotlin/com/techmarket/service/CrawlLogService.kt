package com.techmarket.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages live log streaming for crawlers using Reactor Sinks.
 * 
 * This service provides "direct-best-effort" semantics: logs are emitted to active subscribers
 * but are NOT persisted. If no one is listening, the messages are dropped.
 * 
 * Maintains a subscriber count per company to ensure that underlying [Sinks.Many] are 
 * cleaned up when the last subscriber disconnects, preventing memory leaks.
 */
@Service
class CrawlLogService {
    private val crawlerSinks = Sinks.many().multicast().directBestEffort<CrawlLogMessage>()
    private val companySinks = ConcurrentHashMap<String, Sinks.Many<CrawlLogMessage>>()
    private val subscriberCounts = ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger>()

    data class CrawlLogMessage(
        val companyId: String,
        val level: String,
        val message: String,
        val timestamp: String = Instant.now().toString()
    )

    fun log(companyId: String, level: String, message: String) {
        val msg = CrawlLogMessage(companyId, level, message)
        crawlerSinks.tryEmitNext(msg)
        companySinks[companyId]?.tryEmitNext(msg)
    }

    fun getGlobalStream(): Flux<CrawlLogMessage> = crawlerSinks.asFlux()

    /**
     * Returns a [Flux] of log messages for a specific company.
     * 
     * Increments the subscriber count on subscription and automatically decrements it
     * when the flux terminates (cancel, error, or complete). If the count reaches zero,
     * the company-specific sink is removed from memory.
     */
    fun getCompanyStream(companyId: String): Flux<CrawlLogMessage> {
        subscriberCounts.computeIfAbsent(companyId) { java.util.concurrent.atomic.AtomicInteger(0) }.incrementAndGet()
        
        val sink = companySinks.computeIfAbsent(companyId) {
            Sinks.many().multicast().directBestEffort()
        }
        
        return sink.asFlux()
            .doFinally {
                val count = subscriberCounts[companyId]?.decrementAndGet() ?: 0
                if (count <= 0) {
                    subscriberCounts.remove(companyId)
                    companySinks.remove(companyId)
                }
            }
    }
}
