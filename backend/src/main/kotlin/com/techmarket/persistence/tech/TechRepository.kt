package com.techmarket.persistence.tech

import com.techmarket.api.model.TechDetailsPageDto

interface TechRepository {
    fun getTechDetails(techName: String): TechDetailsPageDto
}
