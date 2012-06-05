/*
 * Moresby Coffee Bean
 *
 * Copyright (c) 2012, Barnabas Sudy (barnabas.sudy@gmail.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the FreeBSD Project.
 */
package org.moresbycoffee.hibernatecache.domain;

import static org.junit.Assert.*;

import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.stat.Statistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.moresbycoffee.hibernatecache.domain.NoCacheEntity;
import org.moresbycoffee.hibernatecache.domain.ReadOnlyEntity;

/**
 * TODO javadoc.
 *
 * @author Barnabas Sudy (barnabas.sudy@gmail.com)
 * @since 2012
 */
public class SecondLevelCache extends EntityManagerTest {

    private EntityManager em1;

    @Before
    public void setUp() {
        em1 = emf.createEntityManager();
    }

    @After
    public void tearDown() {
        em1.close();
    }

    @Test
    public void secondLevelCacheIsNotDefault() {
        final List<NoCacheEntity> entitiesNoChacheEm1 = getNoCacheEntities(em1, "EM1");

        printStat(em1, "EM1");
        {
            final Statistics stats = getStatistics(em1);
            assertLong("2nd level hit count", 0, stats.getSecondLevelCacheHitCount());
            assertLong("2d level put count",  0, stats.getSecondLevelCachePutCount());
        }

        final EntityManager em2 = emf.createEntityManager();
        final List<NoCacheEntity> entitiesNoChacheEm2 = getNoCacheEntities(em2, "EM1");

        printStat(em2, "EM2");
        {
            /* No hit on the second level cache */
            final Statistics stats = getStatistics(em2);
            assertLong("2nd level hit count", 0, stats.getSecondLevelCacheHitCount());
            assertLong("2d level put count",  0, stats.getSecondLevelCachePutCount());
        }

        assertEquals(entitiesNoChacheEm1.size(), entitiesNoChacheEm2.size());

        /* Different instances in the results */
        assertFalse(entitiesNoChacheEm1.equals(entitiesNoChacheEm2));
    }

    @Test
    public void sharedSecondLevelCache() {

        final List<ReadOnlyEntity> entitiesEm1 = getROEntities(em1, "EM1");

        printStat(em1, "EM1");
        {
            /* Entities put into 2nd level cache. */
            final Statistics stats = getStatistics(em1);
            assertLong("2nd level hit count", 0, stats.getSecondLevelCacheHitCount());
            assertLong("2d level put count",  90, stats.getSecondLevelCachePutCount());
        }

        final EntityManager em2 = emf.createEntityManager();
        final List<ReadOnlyEntity> entitiesEm2 = getROEntities(em2, "EM1");

        printStat(em2, "EM2");
        {
            /* Entities have been pulled from 2nd level cache. */
            final Statistics stats = getStatistics(em2);
            assertLong("2nd level hit count", 90, stats.getSecondLevelCacheHitCount());
            assertLong("2d level put count",  90, stats.getSecondLevelCachePutCount());
        }

        assertEquals(entitiesEm1.size(), entitiesEm2.size());

        /* Different instances in the results */
        assertFalse(entitiesEm1.equals(entitiesEm2));

    }

    /**
     * In this test I'm retrieving all the ReadOnlyEntity-s,
     * and put them into the 2nd level cache.
     * After it using the <code>find</code> the hibernate is able
     * to pick up the proper entity from the 2nd level cache.
     */
    @Test
    public void findable() {
        final List<ReadOnlyEntity> entitiesEm1 = getROEntities(em1, "EM1");

        printStat(em1, "EM1");
        /* Entities have put into 2nd level cache. */
        assertStat(em1, 1, 0, 0, 90);

        final EntityManager em2 = emf.createEntityManager();
        final ReadOnlyEntity em2Entity = em2.find(ReadOnlyEntity.class, entitiesEm1.get(0).getId());
        assertNotNull(em2Entity);
        printStat(em2, "EM2");
        /* The entity has been found in the 2nd level cache. */
        assertStat(em1, 0, 0, 1, 0);
        em2.close();

    }

    /**
     * In this test I'm retrieving all the ReadOnlyEntity-s,
     * and put them into the 2nd level cache.
     * After it a second query tries to find one of them with a
     * JPQL expression but Hibernate won't use 2nd level cache
     * it is forwarding the query to the database.
     */
    @Test
    public void noQueryable() {
        final List<ReadOnlyEntity> entitiesEm1 = getROEntities(em1, "EM1");

        /* Entities have put into 2nd level cache. */
        printStat(em1, "EM1");
        assertStat(em1, 1, 0, 0, 90);

        final EntityManager em3 = emf.createEntityManager();
        final ReadOnlyEntity em3Entity = (ReadOnlyEntity) em3.createQuery("select e from ReadOnlyEntity e where e.id = " + entitiesEm1.get(0).getId()).getSingleResult();
        assertNotNull(em3Entity);
        printStat(em3, "EM3");
        /* The 2nd level cache hit won't increase */
        assertStat(em1, 1, 0, 0, 0);
        em3.close();
    }


}
