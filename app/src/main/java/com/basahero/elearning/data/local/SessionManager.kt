package com.basahero.elearning.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// ─────────────────────────────────────────────────────────────────────────────
// SessionManager
// Persists student login session across app restarts using DataStore.
// On first launch: student logs in → session saved here.
// On relaunch: MainActivity reads session → if exists, skip login screen.
// On logout: all keys cleared → back to role select.
//
// Also handles language preference (Step 8 — grade theme stored separately
// but language toggle uses the same DataStore file).
// ─────────────────────────────────────────────────────────────────────────────

// Extension property — creates a single DataStore instance for the app
val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "basahero_session"
)

class SessionManager(private val context: Context) {

    companion object {
        // Student session keys
        val KEY_STUDENT_ID    = stringPreferencesKey("student_id")
        val KEY_STUDENT_NAME  = stringPreferencesKey("student_name")
        val KEY_GRADE_LEVEL   = intPreferencesKey("grade_level")
        val KEY_SECTION       = stringPreferencesKey("section")

        // Teacher session (already handled by Supabase Auth — just store flag)
        val KEY_TEACHER_LOGGED_IN = booleanPreferencesKey("teacher_logged_in")

        // Language preference — "en" or "fil"
        val KEY_LANGUAGE = stringPreferencesKey("language")
    }

    // ── Student session ───────────────────────────────────────────────────────

    suspend fun saveStudentSession(
        studentId: String,
        studentName: String,
        gradeLevel: Int,
        section: String
    ) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_STUDENT_ID]   = studentId
            prefs[KEY_STUDENT_NAME] = studentName
            prefs[KEY_GRADE_LEVEL]  = gradeLevel
            prefs[KEY_SECTION]      = section
        }
    }

    suspend fun clearStudentSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(KEY_STUDENT_ID)
            prefs.remove(KEY_STUDENT_NAME)
            prefs.remove(KEY_GRADE_LEVEL)
            prefs.remove(KEY_SECTION)
        }
    }

    // Returns null if no active session (student needs to log in)
    val studentSession: Flow<StudentSession?> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { prefs ->
            val id = prefs[KEY_STUDENT_ID]
            if (id != null) {
                StudentSession(
                    studentId   = id,
                    studentName = prefs[KEY_STUDENT_NAME] ?: "",
                    gradeLevel  = prefs[KEY_GRADE_LEVEL] ?: 4,
                    section     = prefs[KEY_SECTION] ?: ""
                )
            } else null
        }

    // ── Language preference ───────────────────────────────────────────────────

    suspend fun setLanguage(languageCode: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = languageCode
        }
    }

    val language: Flow<String> = context.sessionDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_LANGUAGE] ?: "en" }

    // ── Teacher session ───────────────────────────────────────────────────────

    suspend fun setTeacherLoggedIn(loggedIn: Boolean) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_TEACHER_LOGGED_IN] = loggedIn
        }
    }

    val isTeacherLoggedIn: Flow<Boolean> = context.sessionDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_TEACHER_LOGGED_IN] ?: false }
}

data class StudentSession(
    val studentId: String,
    val studentName: String,
    val gradeLevel: Int,
    val section: String
)