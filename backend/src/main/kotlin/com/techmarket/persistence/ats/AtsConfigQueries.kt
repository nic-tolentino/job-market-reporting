package com.techmarket.persistence.ats

import com.techmarket.persistence.AtsConfigFields

object AtsConfigQueries {
    fun getSelectAllEnabledSql(dataset: String, table: String): String {
        return "SELECT * FROM `$dataset.$table` WHERE ${AtsConfigFields.ENABLED} = true"
    }

    fun getSelectByCompanyIdSql(dataset: String, table: String): String {
        return "SELECT * FROM `$dataset.$table` WHERE ${AtsConfigFields.COMPANY_ID} = @companyId"
    }
}
