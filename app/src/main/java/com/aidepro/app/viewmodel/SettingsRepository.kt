package com.aidepro.app.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aide_pro_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
        val EDITOR_TAB_SIZE = intPreferencesKey("editor_tab_size")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val RECENT_PROJECTS = stringPreferencesKey("recent_projects")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_THEME] ?: true }
    val useDynamicColor: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }
    val aiApiKey: Flow<String> = context.dataStore.data.map { it[Keys.AI_API_KEY] ?: "" }
    val aiBaseUrl: Flow<String> = context.dataStore.data.map { it[Keys.AI_BASE_URL] ?: "https://api.openai.com/v1" }
    val aiModel: Flow<String> = context.dataStore.data.map { it[Keys.AI_MODEL] ?: "gpt-4o-mini" }
    val editorFontSize: Flow<Int> = context.dataStore.data.map { it[Keys.EDITOR_FONT_SIZE] ?: 14 }
    val editorTabSize: Flow<Int> = context.dataStore.data.map { it[Keys.EDITOR_TAB_SIZE] ?: 4 }
    val wordWrap: Flow<Boolean> = context.dataStore.data.map { it[Keys.WORD_WRAP] ?: false }
    val autoSave: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_SAVE] ?: true }

    suspend fun setDarkTheme(value: Boolean) = context.dataStore.edit { it[Keys.DARK_THEME] = value }
    suspend fun setDynamicColor(value: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = value }
    suspend fun setAiApiKey(value: String) = context.dataStore.edit { it[Keys.AI_API_KEY] = value }
    suspend fun setAiBaseUrl(value: String) = context.dataStore.edit { it[Keys.AI_BASE_URL] = value }
    suspend fun setAiModel(value: String) = context.dataStore.edit { it[Keys.AI_MODEL] = value }
    suspend fun setEditorFontSize(value: Int) = context.dataStore.edit { it[Keys.EDITOR_FONT_SIZE] = value }
    suspend fun setEditorTabSize(value: Int) = context.dataStore.edit { it[Keys.EDITOR_TAB_SIZE] = value }
    suspend fun setWordWrap(value: Boolean) = context.dataStore.edit { it[Keys.WORD_WRAP] = value }
    suspend fun setAutoSave(value: Boolean) = context.dataStore.edit { it[Keys.AUTO_SAVE] = value }

    suspend fun getAiApiKey(): String = aiApiKey.first()
    suspend fun getAiBaseUrl(): String = aiBaseUrl.first()
    suspend fun getAiModel(): String = aiModel.first()

    // Recent projects stored as JSON array of paths
    val recentProjects: Flow<List<ProjectInfo>> = context.dataStore.data.map { prefs ->
        val json = prefs[Keys.RECENT_PROJECTS] ?: "[]"
        parseProjectsJson(json)
    }

    suspend fun addRecentProject(project: ProjectInfo) {
        context.dataStore.edit { prefs ->
            val current = parseProjectsJson(prefs[Keys.RECENT_PROJECTS] ?: "[]").toMutableList()
            current.removeAll { it.path == project.path }
            current.add(0, project)
            prefs[Keys.RECENT_PROJECTS] = serializeProjectsJson(current.take(20))
        }
    }

    suspend fun removeRecentProject(path: String) {
        context.dataStore.edit { prefs ->
            val current = parseProjectsJson(prefs[Keys.RECENT_PROJECTS] ?: "[]").toMutableList()
            current.removeAll { it.path == path }
            prefs[Keys.RECENT_PROJECTS] = serializeProjectsJson(current)
        }
    }

    private fun parseProjectsJson(json: String): List<ProjectInfo> {
        return try {
            // Simple JSON parsing without external library
            if (json == "[]") return emptyList()
            json.trim('[', ']').split("},{").map { entry ->
                val clean = entry.trim('{', '}')
                val fields = clean.split(",").associate { field ->
                    val (k, v) = field.split(":", limit = 2)
                    k.trim('"') to v.trim('"')
                }
                ProjectInfo(
                    name = fields["name"] ?: "",
                    path = fields["path"] ?: "",
                    type = fields["type"] ?: "kotlin",
                    lastOpened = fields["lastOpened"]?.toLongOrNull() ?: 0L
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeProjectsJson(projects: List<ProjectInfo>): String {
        val items = projects.joinToString(",") { p ->
            """{"name":"${p.name}","path":"${p.path}","type":"${p.type}","lastOpened":${p.lastOpened}}"""
        }
        return "[$items]"
    }
}
