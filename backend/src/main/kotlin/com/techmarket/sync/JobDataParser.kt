package com.techmarket.sync

import java.time.LocalDate
import java.time.format.DateTimeParseException
import org.springframework.stereotype.Component

@Component
class JobDataParser {

    private val techKeywords =
            setOf(
                    // Languages
                    "kotlin",
                    "java",
                    "python",
                    "go",
                    "golang",
                    "rust",
                    "c++",
                    "c#",
                    "javascript",
                    "typescript",
                    "ruby",
                    "php",
                    "swift",
                    "objective-c",
                    "scala",
                    "dart",
                    "elixir",
                    "clojure",
                    "haskell",
                    "lua",
                    "perl",
                    "r",
                    "shell",
                    "bash",

                    // Frontend Frameworks & Libs
                    "react",
                    "angular",
                    "vue",
                    "nextjs",
                    "next.js",
                    "svelte",
                    "ember",
                    "backbone",
                    "html",
                    "css",
                    "sass",
                    "less",
                    "tailwind",
                    "bootstrap",
                    "material-ui",
                    "redux",
                    "graphql",

                    // Backend Frameworks
                    "spring",
                    "spring boot",
                    "django",
                    "flask",
                    "fastapi",
                    "node",
                    "nodejs",
                    "node.js",
                    "express",
                    "nest",
                    "nestjs",
                    "ruby on rails",
                    "laravel",
                    "asp.net",
                    "dotnet",
                    ".net",

                    // Mobile
                    "android",
                    "ios",
                    "flutter",
                    "react native",
                    "xamarin",
                    "ionic",
                    "kotlin multiplatform",

                    // Cloud & Infrastructure
                    "aws",
                    "gcp",
                    "azure",
                    "docker",
                    "kubernetes",
                    "k8s",
                    "terraform",
                    "ansible",
                    "chef",
                    "puppet",
                    "jenkins",
                    "github actions",
                    "gitlab ci",
                    "circleci",
                    "travis ci",
                    "linux",
                    "ubuntu",
                    "serverless",
                    "lambda",
                    "cloudformation",

                    // Databases & Storage
                    "sql",
                    "postgresql",
                    "postgres",
                    "mysql",
                    "mongodb",
                    "mongo",
                    "redis",
                    "elasticsearch",
                    "cassandra",
                    "dynamodb",
                    "mariadb",
                    "oracle",
                    "sql server",
                    "sqlite",
                    "couchbase",
                    "neo4j",

                    // Data & Analytics
                    "bigquery",
                    "snowflake",
                    "redshift",
                    "hadoop",
                    "spark",
                    "kafka",
                    "rabbitmq",
                    "activemq",
                    "airflow",
                    "dbt",
                    "databricks",
                    "pandas",
                    "numpy",
                    "scikit-learn",
                    "tensorflow",
                    "pytorch"
            )

    fun determineCountry(location: String?): String {
        if (location == null) return "Unknown"
        val locUpper = location.uppercase()
        return when {
            locUpper.contains("AUSTRALIA") ||
                    locUpper.contains(" Sydney") ||
                    locUpper.contains(" Melbourne") -> "AU"
            locUpper.contains("NEW ZEALAND") ||
                    locUpper.contains(" Auckland") ||
                    locUpper.contains(" Wellington") -> "NZ"
            locUpper.contains("SPAIN") ||
                    locUpper.contains(" Madrid") ||
                    locUpper.contains(" Barcelona") -> "ES"
            else -> "Unknown"
        }
    }

    fun extractWorkModel(location: String?, title: String?): String {
        val locLower = location?.lowercase() ?: ""
        val titleLower = title?.lowercase() ?: ""
        val combined = "$locLower $titleLower"
        return when {
            combined.contains("remote") -> "Remote"
            combined.contains("hybrid") -> "Hybrid"
            else -> "On-site"
        }
    }

    fun extractSeniority(title: String, existingLevel: String?): String {
        val t = title.lowercase()
        return when {
            t.contains("senior") || t.contains(" sr.") || t.contains(" sr ") -> "Senior"
            t.contains("lead") || t.contains("principal") || t.contains("staff") -> "Lead/Principal"
            t.contains("junior") || t.contains(" jr.") || t.contains(" jr ") -> "Junior"
            !existingLevel.isNullOrBlank() -> existingLevel
            else -> "Mid-Level"
        }
    }

    fun extractTechnologies(description: String): List<String> {
        val descLower = description.lowercase()
        return techKeywords
                .filter { keyword -> descLower.contains(Regex("\\b${Regex.escape(keyword)}\\b")) }
                .sorted()
    }

    fun parseSalary(salaryStr: String?): Int? {
        if (salaryStr == null) return null
        return try {
            val digits = salaryStr.replace(Regex("[^0-9]"), "")
            if (digits.isNotBlank()) digits.toInt() else null
        } catch (e: Exception) {
            null
        }
    }

    fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr == null) return null
        return try {
            LocalDate.parse(dateStr)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
