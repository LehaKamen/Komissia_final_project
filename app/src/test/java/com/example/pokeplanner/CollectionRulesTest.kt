package com.example.pokeplanner

import com.example.pokeplanner.data.CollectionRules
import org.junit.Assert.*
import org.junit.Test

class CollectionRulesTest {
 @Test fun rejectsBlankCollectionName(){assertFalse(CollectionRules.validName("   "))}
 @Test fun acceptsTrimmedCollectionName(){assertTrue(CollectionRules.validName("  Kanto  "))}
 @Test fun membershipDependsOnBothIds(){assertTrue(CollectionRules.isMember(1,25,listOf(1L to 25)));assertFalse(CollectionRules.isMember(2,25,listOf(1L to 25)))}
 @Test fun plannedFilterExcludesReady(){assertFalse(CollectionRules.visible("Pikachu","pika","READY",true))}
 @Test fun plannedFilterIncludesPlanned(){assertTrue(CollectionRules.visible("Pikachu","PIKA","PLANNED",true))}
 @Test fun emptyQueryMatchesAnyName(){assertTrue(CollectionRules.visible("Pikachu","",null,false))}
 @Test fun cacheExpiresAfterTtl(){assertTrue(CollectionRules.stale(0,7*3600000L,6))}
 @Test fun freshCacheDoesNotExpire(){assertFalse(CollectionRules.stale(2*3600000L,7*3600000L,6))}
}
