package com.techmarket.contract

/**
 * Utility for extracting field references from Kotlin source code.
 * Used for automated contract testing between SQL queries and mappers.
 */
object FieldExtractor {

    /**
     * Extracts all references to field objects from a specific method in a file.
     * Uses a brace-counting approach to accurately determine method boundaries.
     *
     * @param relativePath File path relative to project root
     * @param methodName Name of the method to target
     * @param fieldObjectNames Names of field objects (e.g., "JobFields", "CompanyFields")
     * @return Set of field names extracted from the method body
     */
    fun extractFieldsFromMethod(
        relativePath: String,
        methodName: String,
        vararg fieldObjectNames: String
    ): Set<String> {
        val file = java.io.File(relativePath)
        if (!file.exists()) {
            throw IllegalStateException("Source file not found: $relativePath (working dir: ${System.getProperty("user.dir")})")
        }

        val source = file.readText()
        val methodBody = getMethodBody(source, methodName) 
            ?: throw IllegalStateException("Method '$methodName' not found in $relativePath")
        
        return extractFields(methodBody, *fieldObjectNames)
    }

    /**
     * Extracts all references to field objects from Kotlin source code strings.
     *
     * @param sourceCode The Kotlin source code to parse
     * @param fieldObjectNames The object names (e.g., "JobFields", "CompanyFields", "CompanyAliases")
     * @return Set of field names (e.g., ["SOURCE", "TITLE", "JOB_ID"])
     */
    fun extractFields(sourceCode: String, vararg fieldObjectNames: String): Set<String> {
        // Strip comments to avoid extracting fields that are not actually used
        val cleanSource = sourceCode
            .replace(Regex("""//.*"""), "") // strip single-line comments
            .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "") // strip block comments
        
        val allFields = mutableSetOf<String>()

        // 1. Detect explicit object access: (JobFields|CompanyFields|CompanyAliases).FIELD_NAME
        if (fieldObjectNames.isNotEmpty()) {
            val objectPattern = fieldObjectNames.joinToString("|")
            val regex = Regex("""($objectPattern)\.([A-Z_]+)""")
            allFields.addAll(regex.findAll(cleanSource).map { "${it.groupValues[1]}.${it.groupValues[2]}" })
        }

        // 2. Detect literal string access: get("field_name")
        val literalRegex = Regex("""\.get\(\s*"([A-Za-z_]+)"\s*\)""")
        allFields.addAll(literalRegex.findAll(cleanSource).map { it.groupValues[1] })

        return allFields
    }

    /**
     * Finds the body of a method by counting braces to handle nesting correctly.
     */
    internal fun getMethodBody(source: String, methodName: String): String? {
        val methodHeaderIndex = source.indexOf("fun $methodName")
        if (methodHeaderIndex == -1) return null
        
        val firstBrace = source.indexOf("{", methodHeaderIndex)
        if (firstBrace == -1) return null

        var braceCount = 1
        var currentIndex = firstBrace + 1
        
        while (braceCount > 0 && currentIndex < source.length) {
            when (source[currentIndex]) {
                '{' -> braceCount++
                '}' -> braceCount--
            }
            currentIndex++
        }

        return source.substring(firstBrace, currentIndex)
    }
}
