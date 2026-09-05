package com.raven.application

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface RavenRoute : NavKey

@Serializable
data object Discovery : RavenRoute

@Serializable
data object Chat : RavenRoute

@Serializable
data object Location : RavenRoute

@Serializable
data object MapRoute : RavenRoute

@Serializable
data object CampRoute : RavenRoute

@Serializable
data object Settings : RavenRoute
