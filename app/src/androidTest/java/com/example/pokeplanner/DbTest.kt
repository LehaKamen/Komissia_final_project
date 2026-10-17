package com.example.pokeplanner
import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.pokeplanner.data.local.*;
import kotlinx.coroutines.flow.first;
import kotlinx.coroutines.test.runTest;
import org.junit.*;
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
@RunWith(AndroidJUnit4::class) class DbTest{lateinit var db:AppDatabase;
    lateinit var d:PokeDao;@Before fun setup(){db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),AppDatabase::class.java).allowMainThreadQueries().build();
        d=db.dao()}@After fun close()=db.close();
    @Test fun cacheReadWrite()=runTest{d.putPokemon(PokemonEntity(25,"Pikachu","","electric",1));assertEquals("Pikachu",d.pokemon(25).first()?.name)};
    @Test fun personalNoDuplicate()=runTest{d.putPersonal(PersonalEntity(25,"PLANNED","a","A"));
        d.putPersonal(PersonalEntity(25,"READY","b","B"));
        assertEquals(1,d.personal().first().size)};
    @Test fun recentOrder()=runTest{d.putRecent(RecentEntity(1,1));
        d.putRecent(RecentEntity(2,2));
        assertEquals(listOf(2,1),d.recent().first().map{it.pokemonId})}}
