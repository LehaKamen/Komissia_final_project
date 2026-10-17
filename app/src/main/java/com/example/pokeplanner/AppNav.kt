package com.example.pokeplanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.pokeplanner.data.local.*
import com.example.pokeplanner.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AppNav(){
 val nav=rememberNavController()
 val destinations=listOf("list" to "Каталог","collections" to "Коллекции","recent" to "История","settings" to "Настройки")
 Scaffold(bottomBar={NavigationBar {
  val route=nav.currentBackStackEntryAsState().value?.destination?.route
  destinations.forEach{(path,label)->NavigationBarItem(
   selected=route==path,onClick={nav.navigate(path){launchSingleTop=true;popUpTo("list")}},
   icon={Icon(imageVector=when(path){"list"->Icons.Filled.Search;"collections"->Icons.Filled.Star;"recent"->Icons.Filled.History;else->Icons.Filled.Settings},contentDescription=label,modifier=Modifier.size(30.dp))},label={Text(label)})}
 }}){outer->
  NavHost(navController=nav,startDestination="list",modifier=Modifier.padding(outer)){
   composable("list"){
    val vm:MainVm=hiltViewModel()
    val st by vm.state.collectAsStateWithLifecycle()
    var q by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().padding(12.dp)){
     Text("PokePlanner",style=MaterialTheme.typography.headlineMedium)
     Spacer(Modifier.height(8.dp))
     OutlinedTextField(q,{q=it;vm.search(it)},label={Text("Поиск покемонов (английские названия)")},modifier=Modifier.fillMaxWidth())
     Row{TextButton(onClick=vm::retry){Text("Обновить каталог")}}
     when(val s=st){
      UiState.Loading->CircularProgressIndicator()
      UiState.Empty->Text("Ничего не найдено. Измените запрос или обновите каталог.")
      is UiState.Error->Column{Text("Ошибка загрузки: ${s.text}");Button(onClick=vm::retry){Text("Повторить")}}
      is UiState.Success->PokemonList(s.items){nav.navigate("detail/${it.id}")}
     }
    }
   }
   composable("detail/{id}",arguments=listOf(navArgument("id"){type=NavType.IntType})){
    val vm:DetailVm=hiltViewModel()
    val p by vm.pokemon.collectAsStateWithLifecycle()
    val u by vm.personal.collectAsStateWithLifecycle()
    val collections by vm.collections.collectAsStateWithLifecycle()
    val members by vm.members.collectAsStateWithLifecycle()
    val pokemon=p
    if(pokemon==null)Box(Modifier.padding(20.dp)){Text("Данные пока не сохранены. Подключитесь к интернету и повторите попытку.")}
    else {
     var note by remember(u){mutableStateOf(u?.note?:"")}
     var team by remember(u){mutableStateOf(u?.collection?:"My Team")}
     var status by remember(u){mutableStateOf(u?.status?:"PLANNED")}
     LazyColumn(Modifier.fillMaxSize().padding(16.dp)){
      item {
       Text(pokemon.name,style=MaterialTheme.typography.headlineMedium)
       AsyncImage(pokemon.image,pokemon.name,Modifier.size(130.dp))
       Text("Тип: ${pokemon.types}")
       Spacer(Modifier.height(12.dp))
       Text("Мои данные о покемоне",style=MaterialTheme.typography.titleMedium)
       OutlinedTextField(team,{team=it},label={Text("Название команды")},modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Next))
       OutlinedTextField(note,{note=it},label={Text("Моя заметка")},modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done))
       Row {listOf("PLANNED","TRAINING","READY").forEach { choice ->
        FilterChip(status==choice,{status=choice},label={Text(when(choice){"PLANNED"->"В планах";"TRAINING"->"Тренирую";else->"Готов"})},modifier=Modifier.padding(end=4.dp))
       }}
       Row {Button(onClick={vm.save(status,note,team)}){Text("Сохранить")}
        TextButton(onClick=vm::remove){Text("Удалить")}}
       HorizontalDivider()
       Text("Добавить в коллекции",style=MaterialTheme.typography.titleMedium)
       if(collections.isEmpty())Text("Сначала создайте коллекцию на вкладке «Коллекции»")
      }
      items(collections,key={it.id}) { collection ->
       val checked=members.any{it.collectionId==collection.id && it.pokemonId==pokemon.id}
       Row(Modifier.fillMaxWidth().clickable{vm.toggleMember(collection.id,checked)}.padding(vertical=5.dp)){
        Checkbox(checked=checked,onCheckedChange={vm.toggleMember(collection.id,checked)})
        Text(collection.name,Modifier.padding(top=12.dp))
       }
      }
     }
    }
   }
   composable("collections"){
    val vm:LibraryVm=hiltViewModel()
    val collections by vm.collections.collectAsStateWithLifecycle()
    val members by vm.members.collectAsStateWithLifecycle()
    val pokemon by vm.pokemon.collectAsStateWithLifecycle()
    val personal by vm.personal.collectAsStateWithLifecycle()
    var name by remember{mutableStateOf("")}
    var description by remember{mutableStateOf("")}
    var editing by remember{mutableStateOf<CollectionEntity?>(null)}
    LazyColumn(Modifier.fillMaxSize().padding(12.dp)){
     item{
      Text("Мои коллекции",style=MaterialTheme.typography.headlineMedium)
      Text("Создавайте списки и добавляйте покемонов из карточки. Данные доступны без интернета.")
      OutlinedTextField(name,{name=it},label={Text("Название коллекции")},modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Next))
      OutlinedTextField(description,{description=it},label={Text("Описание")},modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done))
      Button(enabled=name.isNotBlank(),onClick={vm.create(name,description);name="";description=""}){Text("Создать коллекцию")}
      HorizontalDivider()
      Text("Мои покемоны (${personal.size})",style=MaterialTheme.typography.titleMedium)
     }
     items(personal,key={"personal-${it.pokemonId}"}){entry->
      val p=pokemon.find{it.id==entry.pokemonId}
      ListItem(headlineContent={Text(p?.name?:"Pokémon #${entry.pokemonId}")},
       supportingContent={Text("${when(entry.status){"PLANNED"->"В планах";"TRAINING"->"Тренирую";"READY"->"Готов";else->entry.status}} • ${entry.collection} • ${entry.note}")},
       modifier=Modifier.clickable{nav.navigate("detail/${entry.pokemonId}")})
     }
     item{HorizontalDivider();Text("Коллекции",style=MaterialTheme.typography.titleMedium)}
     items(collections,key={"collection-${it.id}"}){collection->
      val ids=members.filter{it.collectionId==collection.id}.map{it.pokemonId}
      Card(Modifier.fillMaxWidth().padding(vertical=6.dp)){
       Column(Modifier.padding(12.dp)){
        Text(collection.name,style=MaterialTheme.typography.titleMedium)
        Text(collection.description)
        Text("Покемонов: ${ids.size}")
        ids.forEach{id->
         TextButton(onClick={nav.navigate("detail/$id")}){
          Text(pokemon.find{it.id==id}?.name?:"Pokémon #$id")
         }
        }
        Row{
         TextButton(onClick={editing=collection;name=collection.name;description=collection.description}){Text("Изменить")}
         TextButton(onClick={vm.delete(collection.id)}){Text("Удалить")}
        }
       }
      }
     }
     if(editing!=null){item{
      Text("Редактирование: ${editing!!.name}")
      Button(onClick={vm.rename(editing!!,name,description);editing=null;name="";description=""}){Text("Сохранить изменения")}
      TextButton(onClick={editing=null;name="";description=""}){Text("Отмена")}
     }}
    }
   }
   composable("recent"){
    val vm:LibraryVm=hiltViewModel()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val pokemon by vm.pokemon.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(12.dp)){
     Text("Недавно просмотренные",style=MaterialTheme.typography.headlineMedium)
     TextButton(onClick=vm::clearHistory){Text("Очистить историю")}
     if(recent.isEmpty())Text("Вы ещё не просматривали покемонов")
     else LazyColumn{items(recent,key={it.pokemonId}){item->
      val p=pokemon.find{it.id==item.pokemonId}
      ListItem(headlineContent={Text(p?.name?:"Pokémon #${item.pokemonId}")},
       supportingContent={Text("Нажмите, чтобы открыть карточку")},
       modifier=Modifier.clickable{nav.navigate("detail/${item.pokemonId}")})
      HorizontalDivider()
     }}
    }
   }
   composable("settings"){
    val vm:SettingsVm=hiltViewModel()
    val s by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp)){
     Text("Настройки",style=MaterialTheme.typography.headlineMedium)
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Тёмная тема");Switch(s.dark,vm::dark)}
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Только запланированные");Switch(s.onlyPlanned,vm::planned)}
     Text("Срок актуальности кэша: ${s.refreshHours} ч")
     Row{listOf(6,12,24).forEach{hours->FilterChip(s.refreshHours==hours,{vm.hours(hours)},label={Text("${hours} ч")},modifier=Modifier.padding(end=8.dp))}}
     Text("Room хранит каталог, заметки, коллекции и историю. DataStore сохраняет настройки. WorkManager обновляет каталог при наличии интернета.")
     OutlinedButton(onClick=vm::clear){Text("Очистить историю просмотров")}
    }
   }
  }
 }
}

@Composable private fun PokemonList(items:List<PokemonEntity>,onClick:(PokemonEntity)->Unit){
 LazyColumn{items(items,key={it.id}){p->
  ListItem(headlineContent={Text(p.name)},supportingContent={Text(p.types)},
   leadingContent={AsyncImage(p.image,p.name,Modifier.size(56.dp))},
   modifier=Modifier.clickable{onClick(p)})
  HorizontalDivider()
 }}
}
