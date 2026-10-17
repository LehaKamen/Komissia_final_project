package com.example.pokeplanner.ui

import androidx.lifecycle.*
import com.example.pokeplanner.data.*
import com.example.pokeplanner.data.local.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UiState{
 data object Loading:UiState
 data object Empty:UiState
 data class Error(val text:String):UiState
 data class Success(val items:List<PokemonEntity>):UiState
}

@HiltViewModel class MainVm @Inject constructor(private val r:Repository,private val settings:SettingsRepository):ViewModel(){
 private val q=MutableStateFlow("")
 private val loading=MutableStateFlow(true)
 private val error=MutableStateFlow<String?>(null)
 private val refresh=MutableSharedFlow<Unit>(extraBufferCapacity=1)
 val state: StateFlow<UiState> = combine(
  r.pokemon,r.personal,r.members,q.debounce(350).distinctUntilChanged(),settings.flow,loading,error
 ){ values ->
  @Suppress("UNCHECKED_CAST")
  val p=values[0] as List<PokemonEntity>
  @Suppress("UNCHECKED_CAST")
  val u=values[1] as List<PersonalEntity>
  val query=values[3] as String
  val s=values[4] as Settings
  val isLoading=values[5] as Boolean
  val err=values[6] as String?
  val planned=u.filter{it.status=="PLANNED"}.map{it.pokemonId}.toSet()
  val out=p.filter{it.name.contains(query,true)}.filter{!s.onlyPlanned||it.id in planned}
  when {
   isLoading && p.isEmpty()->UiState.Loading
   err!=null && p.isEmpty()->UiState.Error(err)
   out.isEmpty()->UiState.Empty
   else->UiState.Success(out)
  }
 }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),UiState.Loading)
 init {
  viewModelScope.launch {
   refresh.onStart{emit(Unit)}.collect {
    loading.value=true
    error.value=null
    try {
     val hours=settings.flow.first().refreshHours
     if(r.shouldRefresh(hours)) r.refresh()
    }catch(e:Exception){error.value=e.message?:"Network unavailable"}
    finally{loading.value=false}
   }
  }
 }
 fun search(v:String){q.value=v}
 fun retry(){viewModelScope.launch {loading.value=true;error.value=null;try{r.refresh()}catch(e:Exception){error.value=e.message?:"Network unavailable"}finally{loading.value=false}}}
}

@HiltViewModel class DetailVm @Inject constructor(h:SavedStateHandle,private val r:Repository):ViewModel(){
 val id:Int=checkNotNull(h["id"])
 val pokemon=r.detail(id).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
 val personal=r.personal(id).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
 val collections=r.collections.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val members=r.members.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 init{viewModelScope.launch{r.recent(id);if(r.detail(id).first()==null)runCatching{r.ensure(id)}}}
 fun save(s:String,n:String,c:String)=viewModelScope.launch{r.save(id,s,n,c)}
 fun remove()=viewModelScope.launch{r.remove(id)}
 fun toggleMember(collectionId:Long,exists:Boolean)=viewModelScope.launch{
  if(exists)r.removeMember(collectionId,id) else r.addMember(collectionId,id)
 }
}

@HiltViewModel class LibraryVm @Inject constructor(private val r:Repository):ViewModel(){
 val pokemon=r.pokemon.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val personal=r.personal.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val collections=r.collections.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val members=r.members.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val recent=r.recent.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 fun create(name:String,description:String)=viewModelScope.launch{if(name.isNotBlank())r.addCollection(name,description)}
 fun rename(item:CollectionEntity,name:String,description:String)=viewModelScope.launch{if(name.isNotBlank())r.renameCollection(item.copy(name=name.trim(),description=description.trim()))}
 fun delete(id:Long)=viewModelScope.launch{r.deleteCollection(id)}
 fun clearHistory()=viewModelScope.launch{r.clearRecent()}
}

@HiltViewModel class SettingsVm @Inject constructor(private val s:SettingsRepository,private val r:Repository):ViewModel(){
 val state=s.flow.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),Settings())
 fun dark(v:Boolean)=viewModelScope.launch{s.dark(v)}
 fun planned(v:Boolean)=viewModelScope.launch{s.planned(v)}
 fun hours(v:Int)=viewModelScope.launch{s.hours(v)}
 fun clear()=viewModelScope.launch{r.clearRecent()}
}
