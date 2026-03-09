package com.techmarket.contract

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows

class FieldExtractorTest {

    @Test
    fun `extractFields finds all JobFields references`() {
        val sourceCode = """
            fun mapJob(r: FieldValueList) {
                val title = r.get(JobFields.TITLE).stringValue
                val source = r.get(JobFields.SOURCE).stringValue
                val country = r.get(JobFields.COUNTRY).stringValue
                val desc = r.get(CompanyAliases.DESCRIPTION).stringValue
            }
        """.trimIndent()

        val fields = FieldExtractor.extractFields(sourceCode, "JobFields", "CompanyAliases")

        assertEquals(setOf("JobFields.TITLE", "JobFields.SOURCE", "JobFields.COUNTRY", "CompanyAliases.DESCRIPTION"), fields)
    }

    @Test
    fun `getMethodBody handles nested braces correctly`() {
        val sourceCode = """
            fun mapJob(r: FieldValueList) {
                if (condition) {
                    val title = r.get(JobFields.TITLE).stringValue
                    when (type) {
                        "A" -> r.get(JobFields.TYPE_A).stringValue
                        "B" -> {
                            r.get(JobFields.TYPE_B).stringValue
                        }
                    }
                }
                val source = r.get(JobFields.SOURCE).stringValue
            }

            fun nextMethod() { ... }
        """.trimIndent()

        val methodBody = FieldExtractor.getMethodBody(sourceCode, "mapJob")
        assertTrue(methodBody != null)
        assertTrue(methodBody!!.contains("JobFields.TITLE"))
        assertTrue(methodBody!!.contains("JobFields.SOURCE"))
        assertTrue(methodBody!!.endsWith("}"))
        // Check that nextMethod is NOT in mapJob body
        assertTrue(!methodBody!!.contains("fun nextMethod"))
    }

    @Test
    fun `extractFields finds literal string access`() {
        val sourceCode = """
            fun mapLine(row: FieldValueList) {
                val name = row.get("name").stringValue
                val value = row.get("value").longValue
            }
        """.trimIndent()

        val fields = FieldExtractor.extractFields(sourceCode)

        assertEquals(setOf("name", "value"), fields)
    }

    @Test
    fun `extractFields handles multiple object names and line breaks`() {
        val sourceCode = """
            fun mapJob(r: FieldValueList) {
                val title = r.get(JobFields.TITLE).stringValue
                val desc = if (r.get(CompanyAliases.DESCRIPTION).isNull) "" 
                           else r.get(CompanyAliases.DESCRIPTION).stringValue
            }
        """.trimIndent()

        val fields = FieldExtractor.extractFields(sourceCode, "JobFields", "CompanyAliases")

        assertEquals(setOf("JobFields.TITLE", "CompanyAliases.DESCRIPTION"), fields)
    }

    @Test
    fun `extractFields ignores non-matching patterns`() {
        val sourceCode = """
            val jobFields = "something"
            // JobFields.COMMENTED_OUT
            r.get(JobFields.ACTIVE)
        """.trimIndent()

        val fields = FieldExtractor.extractFields(sourceCode, "JobFields")

        assertEquals(setOf("JobFields.ACTIVE"), fields)
    }

    @Test
    fun `extractFieldsFromMethod throws when file not found`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            FieldExtractor.extractFieldsFromMethod(
                "nonexistent/file.kt", 
                "someMethod", 
                "JobFields"
            )
        }
        assertTrue(exception.message!!.contains("Source file not found", ignoreCase = true))
        assertTrue(exception.message!!.contains("working dir: "))
    }

    @Test
    fun `extractFieldsFromMethod throws when method not found`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            // Use this file as it definitely exists
            val path = "src/test/kotlin/com/techmarket/contract/FieldExtractorTest.kt"
            FieldExtractor.extractFieldsFromMethod(
                path, 
                "nonExistentMethodName", 
                "JobFields"
            )
        }
        assertTrue(exception.message!!.contains("Method 'nonExistentMethodName' not found"))
    }
}
