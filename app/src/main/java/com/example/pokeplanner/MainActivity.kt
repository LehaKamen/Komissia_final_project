package com.example.pokeplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pokeplanner.ui.SettingsVm
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  setContent { val vm:SettingsVm=hiltViewModel();val s by vm.state.collectAsStateWithLifecycle()
   MaterialTheme(colorScheme=if(s.dark)darkColorScheme() else lightColorScheme()){AppNav()}
  }
 }
}
