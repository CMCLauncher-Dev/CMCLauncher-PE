package com.cmc.launcher.game.version.mod.meta

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class QuiltModMetadata(
    @SerializedName("schema_version")
    val schemaVersion: Int,
    @SerializedName("quilt_loader")
    val quiltLoader: QuiltLoader
) {
    data class QuiltLoader(
        @SerializedName("id")
        val id: String,
        @SerializedName("version")
        val version: String,
        @SerializedName("metadata")
        val metadata: Metadata,
        @SerializedName("depends")
        val depends: List<Dependency>? = null
    ) {
        data class Dependency(
            @SerializedName("id")
            val id: String,
            @SerializedName("optional")
            val optional: Boolean = false,
            @SerializedName("unless")
            val unless: JsonObject? = null
        )
        data class Metadata(
            @SerializedName("name")
            val name: String,
            @SerializedName("description")
            val description: String,
            @SerializedName("contributors")
            val contributors: JsonObject? = null,
            @SerializedName("icon")
            val icon: String? = null,
            @SerializedName("contact")
            val contact: JsonObject? = null
        )
    }
}
