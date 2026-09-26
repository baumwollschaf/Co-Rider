package com.ralphdietrich.corider.domain

import com.ralphdietrich.corider.domain.model.GpxDocument

interface MapsRouteUrlFactory {
    fun createUrl(document: GpxDocument): String
}

class RouteConversionException(message: String) : IllegalArgumentException(message)
