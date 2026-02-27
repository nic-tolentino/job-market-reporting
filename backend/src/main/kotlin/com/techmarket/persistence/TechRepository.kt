package com.techmarket.persistence

import com.techmarket.api.model.TechDetailsPageDto

interface TechRepository {
    fun getTechDetails(techName: String): TechDetailsPageDto
}
