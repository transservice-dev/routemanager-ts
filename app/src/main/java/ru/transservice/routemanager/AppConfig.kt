package ru.transservice.routemanager

import ru.transservice.routemanager.data.local.entities.FailureReasons

object AppConfig {
    val FAILURE_REASONS = arrayOf(
        FailureReasons.EMPTY_VALUE.reasonTitle,
        FailureReasons.NO_GARBAGE.reasonTitle,
        FailureReasons.CARS_ON_POINT.reasonTitle,
        FailureReasons.ROAD_REPAIR.reasonTitle,
        FailureReasons.DOORS_CLOSED.reasonTitle,
        FailureReasons.CLIENT_DENIAL.reasonTitle,
        FailureReasons.NO_EQUIPMENT.reasonTitle,
        FailureReasons.EQUIPMENT_LOCKED.reasonTitle,
        FailureReasons.WEATHER_CONDITIONS.reasonTitle,
        FailureReasons.OTHER.reasonTitle
    )
}