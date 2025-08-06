package com.jericx.trainr.presentation.common

import android.content.Context
import androidx.core.content.edit

object NavigationStateManager {
    private const val PREFS_NAME = "navigation_state"
    private const val KEY_CURRENT_ROUTE = "current_route"
    private const val KEY_LANGUAGE_CHANGE_PENDING = "language_change_pending"
    
    fun saveCurrentRoute(context: Context, route: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_CURRENT_ROUTE, route)
        }
    }
    
    fun getCurrentRoute(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT_ROUTE, null)
    }
    
    fun setLanguageChangePending(context: Context, pending: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_LANGUAGE_CHANGE_PENDING, pending)
        }
    }
    
    fun isLanguageChangePending(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LANGUAGE_CHANGE_PENDING, false)
    }
    
    fun clearNavigationState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            remove(KEY_CURRENT_ROUTE)
                .remove(KEY_LANGUAGE_CHANGE_PENDING)
        }
    }
}