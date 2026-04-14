package com.taxeca.calculator.domain.model

enum class Province(
    val code: String,
    val nameEn: String,
    val nameFr: String,
    val gstRate: Double,
    val pstRate: Double,
    val hstRate: Double,
    val pstLabel: String
) {
    AB(
        code = "AB",
        nameEn = "Alberta",
        nameFr = "Alberta",
        gstRate = 0.05,
        pstRate = 0.0,
        hstRate = 0.0,
        pstLabel = ""
    ),
    BC(
        code = "BC",
        nameEn = "British Columbia",
        nameFr = "Colombie-Britannique",
        gstRate = 0.05,
        pstRate = 0.07,
        hstRate = 0.0,
        pstLabel = "PST"
    ),
    MB(
        code = "MB",
        nameEn = "Manitoba",
        nameFr = "Manitoba",
        gstRate = 0.05,
        pstRate = 0.07,
        hstRate = 0.0,
        pstLabel = "RST"
    ),
    NB(
        code = "NB",
        nameEn = "New Brunswick",
        nameFr = "Nouveau-Brunswick",
        gstRate = 0.0,
        pstRate = 0.0,
        hstRate = 0.15,
        pstLabel = ""
    ),
    NL(
        code = "NL",
        nameEn = "Newfoundland & Labrador",
        nameFr = "Terre-Neuve-et-Labrador",
        gstRate = 0.0,
        pstRate = 0.0,
        hstRate = 0.15,
        pstLabel = ""
    ),
    NT(
        code = "NT",
        nameEn = "Northwest Territories",
        nameFr = "Territoires du Nord-Ouest",
        gstRate = 0.05,
        pstRate = 0.0,
        hstRate = 0.0,
        pstLabel = ""
    ),
    NS(
        code = "NS",
        nameEn = "Nova Scotia",
        nameFr = "Nouvelle-Écosse",
        gstRate = 0.0,
        pstRate = 0.0,
        hstRate = 0.15,
        pstLabel = ""
    ),
    NU(
        code = "NU",
        nameEn = "Nunavut",
        nameFr = "Nunavut",
        gstRate = 0.05,
        pstRate = 0.0,
        hstRate = 0.0,
        pstLabel = ""
    ),
    ON(
        code = "ON",
        nameEn = "Ontario",
        nameFr = "Ontario",
        gstRate = 0.0,
        pstRate = 0.0,
        hstRate = 0.13,
        pstLabel = ""
    ),
    PE(
        code = "PE",
        nameEn = "Prince Edward Island",
        nameFr = "Île-du-Prince-Édouard",
        gstRate = 0.0,
        pstRate = 0.0,
        hstRate = 0.15,
        pstLabel = ""
    ),
    QC(
        code = "QC",
        nameEn = "Quebec",
        nameFr = "Québec",
        gstRate = 0.05,
        pstRate = 0.09975,
        hstRate = 0.0,
        pstLabel = "QST"
    ),
    SK(
        code = "SK",
        nameEn = "Saskatchewan",
        nameFr = "Saskatchewan",
        gstRate = 0.05,
        pstRate = 0.06,
        hstRate = 0.0,
        pstLabel = "PST"
    ),
    YT(
        code = "YT",
        nameEn = "Yukon",
        nameFr = "Yukon",
        gstRate = 0.05,
        pstRate = 0.0,
        hstRate = 0.0,
        pstLabel = ""
    );

    val isHstProvince: Boolean
        get() = hstRate > 0.0

    val totalRate: Double
        get() = if (isHstProvince) hstRate else gstRate + pstRate

    companion object {
        fun fromCode(code: String): Province =
            entries.firstOrNull { it.code == code } ?: QC
    }
}
