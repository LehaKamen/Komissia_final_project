package com.example.pokeplanner.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ds by preferencesDataStore("settings")
data class Settings(val dark:Boolean=false,val onlyPlanned:Boolean=false,val refreshHours:Int=12)
@Singleton class SettingsRepository @Inject constructor(@ApplicationContext private val c:Context){
 private val darkKey=booleanPreferencesKey("dark")
 private val plannedKey=booleanPreferencesKey("planned")
 private val hoursKey=intPreferencesKey("refresh_hours")
 val flow=c.ds.data.map { Settings(it[darkKey]?:false,it[plannedKey]?:false,it[hoursKey]?:12) }
 suspend fun dark(v:Boolean){c.ds.edit{it[darkKey]=v}}
 suspend fun planned(v:Boolean){c.ds.edit{it[plannedKey]=v}}
 suspend fun hours(v:Int){c.ds.edit{it[hoursKey]=v}}
}
