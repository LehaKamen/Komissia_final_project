package com.example.pokeplanner

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pokeplanner.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class CollectionsIntegrationTest {
 private lateinit var db:AppDatabase
 private lateinit var dao:PokeDao
 @Before fun setup(){db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),AppDatabase::class.java).build();dao=db.dao()}
 @After fun close(){db.close()}
 @Test fun createCollectionAndAddMember()=runBlocking {
  val id=dao.addCollection(CollectionEntity(name="Kanto"))
  dao.addMember(CollectionMemberEntity(id,25))
  assertEquals("Kanto",dao.collections().first().single().name)
  assertEquals(25,dao.members().first().single().pokemonId)
 }
 @Test fun repeatedMemberDoesNotDuplicate()=runBlocking {
  val id=dao.addCollection(CollectionEntity(name="Kanto"))
  dao.addMember(CollectionMemberEntity(id,25));dao.addMember(CollectionMemberEntity(id,25))
  assertEquals(1,dao.members().first().size)
 }
 @Test fun deletingCollectionCascadesToMembership()=runBlocking {
  val id=dao.addCollection(CollectionEntity(name="Kanto"))
  dao.addMember(CollectionMemberEntity(id,25))
  dao.deleteCollection(id)
  assertTrue(dao.members().first().isEmpty())
 }
}
