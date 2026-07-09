package pt.isel.http

object Uris {
    const val PREFIX = "/api"
    const val HOME = PREFIX

    object Users {
        const val CREATE = "$PREFIX/users"
        const val LIST = "$PREFIX/users"
        const val LOGIN = "$PREFIX/users/login"
        const val ME = "$PREFIX/users/me"
        const val LOGOUT = "$PREFIX/users/logout"
        const val GET_BY_ID = "$PREFIX/users/{id}"
        const val DELETE_BY_ID = "$PREFIX/users/{id}"
    }

    object Cases {
        const val CREATE = "$PREFIX/cases"
        const val LIST = "$PREFIX/cases"
        const val GET_BY_ID = "$PREFIX/cases/{id}"
        const val UPDATE_BY_ID = "$PREFIX/cases/{id}"
        const val DELETE_BY_ID = "$PREFIX/cases/{id}"
        const val LIST_BY_USER_ID = "$PREFIX/users/{userId}/cases"
        const val WEATHER = "$PREFIX/cases/{caseId}/weather"
        const val SCENE = "$PREFIX/cases/{caseId}/scene"
        const val VEHICLES = "$PREFIX/cases/{caseId}/vehicles"
        const val EVIDENCE = "$PREFIX/cases/{caseId}/evidence"
        const val ANALYSES = "$PREFIX/cases/{caseId}/analyses"
    }

    object Vehicles {
        const val GET_BY_ID = "$PREFIX/vehicles/{vehicleId}"
        const val UPDATE_BY_ID = "$PREFIX/vehicles/{vehicleId}"
        const val DELETE_BY_ID = "$PREFIX/vehicles/{vehicleId}"
        const val DAMAGES = "$PREFIX/vehicles/{vehicleId}/damages"
    }

    object Damages {
        const val GET_BY_ID = "$PREFIX/damages/{damageId}"
        const val UPDATE_BY_ID = "$PREFIX/damages/{damageId}"
        const val DELETE_BY_ID = "$PREFIX/damages/{damageId}"
    }

    object Evidence {
        const val GET_BY_ID = "$PREFIX/evidence/{evidenceId}"
        const val UPDATE_BY_ID = "$PREFIX/evidence/{evidenceId}"
        const val DELETE_BY_ID = "$PREFIX/evidence/{evidenceId}"
        const val IMAGE = "$PREFIX/evidence/{evidenceId}/image"
        const val IMAGE_CONTENT = "$PREFIX/evidence/{evidenceId}/image/content"
    }

    object Analyses {
        const val GET_BY_ID = "$PREFIX/analyses/{analysisId}"
        const val DELETE_BY_ID = "$PREFIX/analyses/{analysisId}"
        const val IMAGES = "$PREFIX/analyses/{analysisId}/images"
        const val IMAGE_BY_EVIDENCE_ID = "$PREFIX/analyses/{analysisId}/images/{evidenceId}"
        const val MEASUREMENTS = "$PREFIX/analyses/{analysisId}/measurements"
        const val DAMAGE_COMPARISONS = "$PREFIX/analyses/{analysisId}/damage-comparisons"
        const val CONCLUSION = "$PREFIX/analyses/{analysisId}/conclusion"
        const val REPORTS = "$PREFIX/analyses/{analysisId}/reports"
    }

    object Measurements {
        const val GET_BY_ID = "$PREFIX/measurements/{measurementId}"
        const val DELETE_BY_ID = "$PREFIX/measurements/{measurementId}"
        const val COMPARISON_IMAGE = "$PREFIX/measurements/{measurementId}/comparison-image"
    }

    object DamageComparisons {
        const val GET_BY_ID = "$PREFIX/damage-comparisons/{comparisonId}"
        const val UPDATE_BY_ID = "$PREFIX/damage-comparisons/{comparisonId}"
        const val DELETE_BY_ID = "$PREFIX/damage-comparisons/{comparisonId}"
    }

    object Reports {
        const val LIST = "$PREFIX/reports"
        const val GET_BY_ID = "$PREFIX/reports/{reportId}"
        const val UPDATE_BY_ID = "$PREFIX/reports/{reportId}"
        const val DELETE_BY_ID = "$PREFIX/reports/{reportId}"
    }
}
