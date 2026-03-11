package com.techmarket.util

import com.techmarket.model.TechCategory

/**
 * Centralized technology name formatting and metadata lookup.
 * Provides consistent display names, categories, and brand colors for all technologies.
 * 
 * Note: Tech keys (map keys) are lowercase aliases used for matching raw job data.
 * Display names are the canonical human-readable names returned by format().
 */
object TechFormatter {
    data class TechMetadata(
        val displayName: String,
        val category: TechCategory,
        val brandColor: String? = null
    )

    private val techMetadataMap = mapOf(
        // Languages
        "kotlin" to TechMetadata("Kotlin", TechCategory.LANGUAGES, "#7F52FF"),
        "java" to TechMetadata("Java", TechCategory.LANGUAGES, "#007396"),
        "python" to TechMetadata("Python", TechCategory.LANGUAGES, "#3776AB"),
        "go" to TechMetadata("Go", TechCategory.LANGUAGES, "#00ADD8"),
        "golang" to TechMetadata("Go", TechCategory.LANGUAGES, "#00ADD8"),
        "rust" to TechMetadata("Rust", TechCategory.LANGUAGES, "#DEA584"),
        "c++" to TechMetadata("C++", TechCategory.LANGUAGES, "#00599C"),
        "c#" to TechMetadata("C#", TechCategory.LANGUAGES, "#239120"),
        "javascript" to TechMetadata("JavaScript", TechCategory.LANGUAGES, "#F7DF1E"),
        "typescript" to TechMetadata("TypeScript", TechCategory.LANGUAGES, "#3178C6"),
        "ruby" to TechMetadata("Ruby", TechCategory.LANGUAGES, "#CC342D"),
        "php" to TechMetadata("PHP", TechCategory.LANGUAGES, "#777BB4"),
        "swift" to TechMetadata("Swift", TechCategory.LANGUAGES, "#F05138"),
        "objective-c" to TechMetadata("Objective-C", TechCategory.LANGUAGES, "#4289C1"),
        "scala" to TechMetadata("Scala", TechCategory.LANGUAGES, "#DC322F"),
        "dart" to TechMetadata("Dart", TechCategory.LANGUAGES, "#00B4AB"),
        "elixir" to TechMetadata("Elixir", TechCategory.LANGUAGES, "#4B275F"),
        "clojure" to TechMetadata("Clojure", TechCategory.LANGUAGES, "#5881D8"),
        "haskell" to TechMetadata("Haskell", TechCategory.LANGUAGES, "#5D4F85"),
        "lua" to TechMetadata("Lua", TechCategory.LANGUAGES, "#2C2D72"),
        "perl" to TechMetadata("Perl", TechCategory.LANGUAGES, "#0298C3"),
        "r" to TechMetadata("R", TechCategory.LANGUAGES, "#276DC3"),
        "shell" to TechMetadata("Shell", TechCategory.LANGUAGES, null),
        "bash" to TechMetadata("Bash", TechCategory.LANGUAGES, "#4EAA25"),
        
        // Frontend
        "react" to TechMetadata("React", TechCategory.FRONTEND, "#61DAFB"),
        "angular" to TechMetadata("Angular", TechCategory.FRONTEND, "#DD0031"),
        "vue" to TechMetadata("Vue.js", TechCategory.FRONTEND, "#4FC08D"),
        "vue.js" to TechMetadata("Vue.js", TechCategory.FRONTEND, "#4FC08D"),
        "nextjs" to TechMetadata("Next.js", TechCategory.FRONTEND, "#000000"),
        "next.js" to TechMetadata("Next.js", TechCategory.FRONTEND, "#000000"),
        "svelte" to TechMetadata("Svelte", TechCategory.FRONTEND, "#FF3E00"),
        "ember" to TechMetadata("Ember.js", TechCategory.FRONTEND, "#E04E37"),
        "ember.js" to TechMetadata("Ember.js", TechCategory.FRONTEND, "#E04E37"),
        "backbone" to TechMetadata("Backbone.js", TechCategory.FRONTEND, "#0071B5"),
        "backbone.js" to TechMetadata("Backbone.js", TechCategory.FRONTEND, "#0071B5"),
        "html" to TechMetadata("HTML", TechCategory.FRONTEND, "#E34F26"),
        "css" to TechMetadata("CSS", TechCategory.FRONTEND, "#1572B6"),
        "sass" to TechMetadata("Sass", TechCategory.FRONTEND, "#CC6699"),
        "less" to TechMetadata("Less", TechCategory.FRONTEND, "#1D365D"),
        "tailwind" to TechMetadata("Tailwind CSS", TechCategory.FRONTEND, "#38B2AC"),
        "tailwind css" to TechMetadata("Tailwind CSS", TechCategory.FRONTEND, "#38B2AC"),
        "bootstrap" to TechMetadata("Bootstrap", TechCategory.FRONTEND, "#7952B3"),
        "material-ui" to TechMetadata("Material UI", TechCategory.FRONTEND, "#007FFF"),
        "material ui" to TechMetadata("Material UI", TechCategory.FRONTEND, "#007FFF"),
        "redux" to TechMetadata("Redux", TechCategory.FRONTEND, "#764ABC"),
        
        // Backend
        "spring" to TechMetadata("Spring", TechCategory.BACKEND, "#6DB33F"),
        "spring boot" to TechMetadata("Spring Boot", TechCategory.BACKEND, "#6DB33F"),
        "django" to TechMetadata("Django", TechCategory.BACKEND, "#092E20"),
        "flask" to TechMetadata("Flask", TechCategory.BACKEND, "#000000"),
        "fastapi" to TechMetadata("FastAPI", TechCategory.BACKEND, "#009688"),
        "node" to TechMetadata("Node.js", TechCategory.BACKEND, "#339933"),
        "nodejs" to TechMetadata("Node.js", TechCategory.BACKEND, "#339933"),
        "node.js" to TechMetadata("Node.js", TechCategory.BACKEND, "#339933"),
        "express" to TechMetadata("Express", TechCategory.BACKEND, "#000000"),
        "nest" to TechMetadata("NestJS", TechCategory.BACKEND, "#E0234E"),
        "nestjs" to TechMetadata("NestJS", TechCategory.BACKEND, "#E0234E"),
        "ruby on rails" to TechMetadata("Ruby on Rails", TechCategory.BACKEND, "#D30001"),
        "laravel" to TechMetadata("Laravel", TechCategory.BACKEND, "#FF2D20"),
        "asp.net" to TechMetadata("ASP.NET", TechCategory.BACKEND, "#512BD4"),
        "dotnet" to TechMetadata(".NET", TechCategory.BACKEND, "#512BD4"),
        ".net" to TechMetadata(".NET", TechCategory.BACKEND, "#512BD4"),
        "graphql" to TechMetadata("GraphQL", TechCategory.BACKEND, "#E10098"),
        
        // Mobile
        "android" to TechMetadata("Android", TechCategory.MOBILE, "#3DDC84"),
        "ios" to TechMetadata("iOS", TechCategory.MOBILE, "#000000"),
        "flutter" to TechMetadata("Flutter", TechCategory.MOBILE, "#02569B"),
        "react native" to TechMetadata("React Native", TechCategory.MOBILE, "#61DAFB"),
        "xamarin" to TechMetadata("Xamarin", TechCategory.MOBILE, "#3498DB"),
        "ionic" to TechMetadata("Ionic", TechCategory.MOBILE, "#3880FF"),
        "kotlin multiplatform" to TechMetadata("Kotlin Multiplatform", TechCategory.MOBILE, "#7F52FF"),
        
        // Cloud & Infra
        "aws" to TechMetadata("AWS", TechCategory.CLOUD_INFRA, "#FF9900"),
        "gcp" to TechMetadata("GCP", TechCategory.CLOUD_INFRA, "#4285F4"),
        "azure" to TechMetadata("Azure", TechCategory.CLOUD_INFRA, "#0078D4"),
        "terraform" to TechMetadata("Terraform", TechCategory.CLOUD_INFRA, "#7B42BC"),
        "ansible" to TechMetadata("Ansible", TechCategory.CLOUD_INFRA, "#EE0000"),
        "chef" to TechMetadata("Chef", TechCategory.CLOUD_INFRA, "#F09820"),
        "puppet" to TechMetadata("Puppet", TechCategory.CLOUD_INFRA, "#FFAE1A"),
        "serverless" to TechMetadata("Serverless", TechCategory.CLOUD_INFRA, "#FD5750"),
        "lambda" to TechMetadata("AWS Lambda", TechCategory.CLOUD_INFRA, "#FF9900"),
        "aws lambda" to TechMetadata("AWS Lambda", TechCategory.CLOUD_INFRA, "#FF9900"),
        "cloudformation" to TechMetadata("CloudFormation", TechCategory.CLOUD_INFRA, "#FF9900"),
        
        // Data & AI
        "sql" to TechMetadata("SQL", TechCategory.DATA_AI, "#003B57"),
        "postgresql" to TechMetadata("PostgreSQL", TechCategory.DATA_AI, "#336791"),
        "postgres" to TechMetadata("PostgreSQL", TechCategory.DATA_AI, "#336791"),
        "mysql" to TechMetadata("MySQL", TechCategory.DATA_AI, "#4479A1"),
        "mongodb" to TechMetadata("MongoDB", TechCategory.DATA_AI, "#47A248"),
        "mongo" to TechMetadata("MongoDB", TechCategory.DATA_AI, "#47A248"),
        "redis" to TechMetadata("Redis", TechCategory.DATA_AI, "#DC382D"),
        "elasticsearch" to TechMetadata("Elasticsearch", TechCategory.DATA_AI, "#005571"),
        "cassandra" to TechMetadata("Cassandra", TechCategory.DATA_AI, "#1185B0"),
        "dynamodb" to TechMetadata("DynamoDB", TechCategory.DATA_AI, "#4053D6"),
        "mariadb" to TechMetadata("MariaDB", TechCategory.DATA_AI, "#003B45"),
        "oracle" to TechMetadata("Oracle", TechCategory.DATA_AI, "#C74634"),
        "sql server" to TechMetadata("SQL Server", TechCategory.DATA_AI, "#0078D4"),
        "sqlite" to TechMetadata("SQLite", TechCategory.DATA_AI, "#003B57"),
        "couchbase" to TechMetadata("Couchbase", TechCategory.DATA_AI, "#EA2328"),
        "neo4j" to TechMetadata("Neo4j", TechCategory.DATA_AI, "#4581C3"),
        "bigquery" to TechMetadata("BigQuery", TechCategory.DATA_AI, "#4285F4"),
        "snowflake" to TechMetadata("Snowflake", TechCategory.DATA_AI, "#29B5E8"),
        "redshift" to TechMetadata("Redshift", TechCategory.DATA_AI, "#8C4FFF"),
        "hadoop" to TechMetadata("Hadoop", TechCategory.DATA_AI, "#FFFF00"),
        "spark" to TechMetadata("Apache Spark", TechCategory.DATA_AI, "#E25A1C"),
        "apache spark" to TechMetadata("Apache Spark", TechCategory.DATA_AI, "#E25A1C"),
        "kafka" to TechMetadata("Apache Kafka", TechCategory.DATA_AI, "#231F20"),
        "apache kafka" to TechMetadata("Apache Kafka", TechCategory.DATA_AI, "#231F20"),
        "rabbitmq" to TechMetadata("RabbitMQ", TechCategory.DATA_AI, "#FF6600"),
        "activemq" to TechMetadata("ActiveMQ", TechCategory.DATA_AI, "#D94436"),
        "airflow" to TechMetadata("Apache Airflow", TechCategory.DATA_AI, "#017CEE"),
        "apache airflow" to TechMetadata("Apache Airflow", TechCategory.DATA_AI, "#017CEE"),
        "dbt" to TechMetadata("dbt", TechCategory.DATA_AI, "#FF694B"),
        "databricks" to TechMetadata("Databricks", TechCategory.DATA_AI, "#FF3621"),
        "pandas" to TechMetadata("Pandas", TechCategory.DATA_AI, "#150458"),
        "numpy" to TechMetadata("NumPy", TechCategory.DATA_AI, "#4D77CF"),
        "scikit-learn" to TechMetadata("scikit-learn", TechCategory.DATA_AI, "#F7931E"),
        "tensorflow" to TechMetadata("TensorFlow", TechCategory.DATA_AI, "#FF6F00"),
        "pytorch" to TechMetadata("PyTorch", TechCategory.DATA_AI, "#EE4C2C"),
        
        // DevOps
        "docker" to TechMetadata("Docker", TechCategory.DEVOPS, "#2496ED"),
        "kubernetes" to TechMetadata("Kubernetes", TechCategory.DEVOPS, "#326CE5"),
        "k8s" to TechMetadata("Kubernetes", TechCategory.DEVOPS, "#326CE5"),
        "jenkins" to TechMetadata("Jenkins", TechCategory.DEVOPS, "#D24939"),
        "github actions" to TechMetadata("GitHub Actions", TechCategory.DEVOPS, "#2088FF"),
        "gitlab ci" to TechMetadata("GitLab CI", TechCategory.DEVOPS, "#FC6D26"),
        "circleci" to TechMetadata("CircleCI", TechCategory.DEVOPS, "#343434"),
        "travis ci" to TechMetadata("Travis CI", TechCategory.DEVOPS, "#3EAAAF"),
        "linux" to TechMetadata("Linux", TechCategory.DEVOPS, "#FCC624"),
        "ubuntu" to TechMetadata("Ubuntu", TechCategory.DEVOPS, "#E95420"),
        
        // Security
        "snyk" to TechMetadata("Snyk", TechCategory.SECURITY, "#700578"),
        "auth0" to TechMetadata("Auth0", TechCategory.SECURITY, "#EB5424"),
        "okta" to TechMetadata("Okta", TechCategory.SECURITY, "#007DC1"),
        "cybersecurity" to TechMetadata("Cybersecurity", TechCategory.SECURITY, null)
    )

    /**
     * Formats a technology name to its canonical display name.
     * @param tech the raw technology name (case-insensitive)
     * @return the formatted display name, or title-cased input if not found
     */
    fun format(tech: String): String {
        return techMetadataMap[tech.lowercase()]?.displayName 
            ?: tech.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    /**
     * Retrieves full metadata for a technology.
     * @param tech the raw technology name (case-insensitive)
     * @return TechMetadata with display name, category, and brand color
     */
    fun getMetadata(tech: String): TechMetadata {
        return techMetadataMap[tech.lowercase()]
            ?: TechMetadata(format(tech), TechCategory.LANGUAGES)
    }

    /**
     * Gets the category for a specific technology.
     * @param tech the raw technology name (case-insensitive)
     * @return the TechCategory, or null if unknown
     */
    fun getCategory(tech: String): TechCategory? {
        return techMetadataMap[tech.lowercase()]?.category
    }

    /**
     * Returns all official technology display names in the system.
     * @return set of canonical display names (e.g., "React", "Node.js")
     */
    fun getAllOfficialNames(): Set<String> {
        return techMetadataMap.values.map { it.displayName }.toSet()
    }

    /**
     * Returns all technology keys (aliases) belonging to a specific category.
     * These are lowercase matching keys used for BigQuery lookups, not display names.
     * Example: For FRONTEND returns ["react", "angular", "vue", "vue.js", "nextjs", "next.js", ...]
     * 
     * @param category the TechCategory to filter by
     * @return set of lowercase technology keys in that category
     */
    fun getTechKeysForCategory(category: TechCategory): Set<String> {
        return techMetadataMap
            .filter { it.value.category == category }
            .keys
    }

    /**
     * Counts distinct technologies in a category by their display name.
     */
    fun countDistinctTechsByCategory(category: TechCategory): Int {
        return techMetadataMap.values
            .filter { it.category == category }
            .map { it.displayName }
            .distinct()
            .size
    }

    /**
     * Returns all categories with their technology counts.
     * Counts distinct technologies (by display name), ignoring aliases.
     * @return map of TechCategory to count of distinct technologies
     */
    fun getCategoryCounts(): Map<TechCategory, Int> {
        return TechCategory.entries.associateWith { category ->
            techMetadataMap.values
                .filter { it.category == category }
                .map { it.displayName }
                .distinct()
                .size
        }
    }
}

