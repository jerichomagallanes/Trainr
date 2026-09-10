package com.jericx.trainr.data.catalog

import android.content.Context
import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.ExerciseCatalog

// Read once, on first use, off the asset that ships with the app. A missing or
// broken file leaves an empty catalog rather than a crash: generation then
// fails to find movements and says so, which is recoverable, where a dead
// launch is not.
class AssetExerciseCatalog(
    private val context: Context,
    private val assetName: String = ASSET
) : ExerciseCatalog {

    private val loaded: ExerciseCatalog by lazy {
        runCatching {
            ExerciseCatalogReader.read(
                context.assets.open(assetName).bufferedReader().use { it.readText() }
            )
        }.getOrElse { ExerciseCatalogReader.read("{\"exercises\":[]}") }
    }

    override val all: List<CatalogExercise> get() = loaded.all

    override fun get(key: String): CatalogExercise? = loaded[key]

    private companion object {
        const val ASSET = "exercise-catalog.json"
    }
}
