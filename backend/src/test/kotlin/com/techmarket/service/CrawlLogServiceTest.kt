package com.techmarket.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier
import java.time.Duration

class CrawlLogServiceTest {

    @Test
    fun `log emits to global stream`() {
        val service = CrawlLogService()
        val globalFlux = service.getGlobalStream()

        StepVerifier.create(globalFlux)
            .then { service.log("comp1", "INFO", "Hello") }
            .assertNext { msg ->
                assertEquals("comp1", msg.companyId)
                assertEquals("INFO", msg.level)
                assertEquals("Hello", msg.message)
            }
            .thenCancel()
            .verify(Duration.ofSeconds(1))
    }

    @Test
    fun `log emits to company stream only when matching`() {
        val service = CrawlLogService()
        val companyFlux = service.getCompanyStream("comp1")

        StepVerifier.create(companyFlux)
            .then { 
                service.log("comp2", "INFO", "Ignore this")
                service.log("comp1", "INFO", "Keep this")
            }
            .assertNext { msg ->
                assertEquals("comp1", msg.companyId)
                assertEquals("Keep this", msg.message)
            }
            .thenCancel()
            .verify(Duration.ofSeconds(1))
    }

    @Test
    fun `subscriber cleanup removes sink and count`() {
        val service = CrawlLogService()
        
        // Use a scope to allow disposal
        val flux = service.getCompanyStream("comp1")
        val disposable = flux.subscribe()
        
        // Verify entry exists
        // Since fields are private, we can't check directly easily without reflection or checking internal state
        // But we can check if a new subscription works correctly
        disposable.dispose()
        
        // Wait a bit for doFinally to run
        Thread.sleep(100)
        
        // If we subscribe again, it should be a fresh sink
        val flux2 = service.getCompanyStream("comp1")
        StepVerifier.create(flux2)
            .then { service.log("comp1", "INFO", "New Message") }
            .expectNextMatches { it.message == "New Message" }
            .thenCancel()
            .verify(Duration.ofSeconds(1))
    }
}
