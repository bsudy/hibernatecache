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
package com.moresby.hibernatecache.domain;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import org.apache.log4j.Logger;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.junit.Assert;
import org.junit.Test;

/**
 * This class can test the behavior of the Hibernate 2nd level cache
 * with {@link CacheConcurrencyStrategy#READ_ONLY read-only} entities.
 *
 * @author Barnabas Sudy (barnabas.sudy@gmail.com)
 * @since 2012
 */
public class ReadOnlyTest extends EntityManagerTest {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(ReadOnlyTest.class);

    @Test
    public void addReadOnlyEntityCacheTest() {
        //Tests the read only cached entities.
        LOG.info("---------------------------------------------------------------------");
        LOG.info("------------------------- READ ONLY TEST ----------------------------");
        LOG.info("---------------------------------------------------------------------");

        final EntityManager em1 = emf.createEntityManager();
        final EntityManager em2 = emf.createEntityManager();

        /* Starts transaction */
        final EntityTransaction transaction1Em1 = em1.getTransaction();
        transaction1Em1.begin();

        LOG.info("*** STEP 1");
        /*
         * Hibernate retrieves the entities from the database
         * and adds them to the 2nd level cache.
         * Query cache: New query added.
         * 2nd level cache: Entities from the result added.
         */
        countEntities(em1, "EM1");
        printStat(em1, "EM1");
        assertStat(em1, 1, 0, 0, 90);

        LOG.info("*** STEP 2");
        /*
         * The new query from a different session does not hit the
         * database because it can find everything in the 2nd level cache.
         * Query cache: The query retrieved
         * 2nd level cache: The entities are retrieved.
         */
        countEntities(em2, "EM2");
        printStat(em2, "EM2");
        assertStat(em2, 0, 1, 90, 0);

        LOG.info("*** STEP 3");
        /*
         * Still everything in the second level cache. Query cache is in use
         * but entity 2nd level cache no longer used because everything in the
         * 1st level cache.
         * Query cache: In use
         * 2nd level cache: 1st level cache instead of the 2nd level cache.
         */
        countEntities(em1, "EM1");
        printStat(em1, "EM1");
        assertStat(em1, 0, 1, 0, 0);


        LOG.info("*** STEP 4");
        /*
         * A new EntityManager is created which uses the query and
         * the  2nd level cache.
         */
        {
            final EntityManager em3 = emf.createEntityManager();
            /*
             * Query cache: In use
             * 2nd level cache: In use.
             */
            countEntities(em3, "EM3");
            printStat(em3, "EM3");
            assertStat(em1, 0, 1, 90, 0);

            em3.close();
        }

        LOG.info("*** STEP 5");
        LOG.info("-------- INSERT NEW READ ONLY ENTITY -----------");
        em1.persist(new ReadOnlyEntity("newEntity"));
        em1.flush();
        assertStat(em1, 1, 0, 0, 0);

        LOG.info("*** STEP 6");
        /*
         * The 2nd level cache is in invalid we added a new entity to a table and
         * the hibernate doesn't know what would be the result of the query without
         * the database.
         * Query cache: Not in use
         * 2nd level cache: Not in use
         */
        countEntities(em1, "EM1");
        printStat(em1, "EM1");
        assertStat(em1, 1, 0, 0, 0);

//        em1.clear();
        LOG.info("*** STEP 7");
        /*
         * The same problem.
         * Query cache: Not in use
         * 2nd level cache: Not in use
         */
        countEntities(em1, "EM1");
        printStat(em1, "EM1");
        assertStat(em1, 1, 0, 0, 0);

        LOG.info("*** STEP 8");
        /*
         * Here the em2 can't reach the database because the database is locked.
         * The result at this point is database dependent. The h2 database throws an exception
         * so I'll catch it.
         * The 2nd level cache is skipped.
         */
        {
            final EntityManager em3 = emf.createEntityManager();
            /*
             * Query cache: In use
             * 2nd level cache: In use
             */
            try {
                countEntities(em3, "EM1");
                Assert.fail("An exception should have been thrown.");
            } catch (final PersistenceException e) {
                LOG.info("The database table is locked. " + e.getMessage());
            }

            em3.close();
        }

        LOG.info("*** STEP 9 - COMMIT");
        transaction1Em1.commit();

        LOG.info("*** STEP 10");
        /*
         * Runs the query on the database and stores the new entity in the 2nd level cache.
         * Query cache: New result added.
         * 2nd level cache: New entity added.
         */
        countEntities(em2, "EM2");
        printStat(em2, "EM2");
        assertStat(em1, 2, 0, 0, 1);


        LOG.info("*** STEP 11");
        /*
         * Uses the 2nd level cache and the query cache.
         * Query cache: in use.
         * 2nd level cache: in use.
         */
        {
            final EntityManager em3 = emf.createEntityManager();
            /*
             * Query cache: In use
             * 2nd level cache: In use
             */
            countEntities(em3, "EM1");
            printStat(em3, "EM1");
            assertStat(em1, 0, 1, 91, 0);

            em3.close();
        }

        em2.close();
        em1.close();

    }

    @Test
    public void updateReadWriteEntityCacheTest() {
        //Tests the read write cached entities.
        LOG.info("---------------------------------------------------------------------");
        LOG.info("------------------- READ ONLY UPDATE TEST ---------------------------");
        LOG.info("---------------------------------------------------------------------");

        final EntityManager em1 = emf.createEntityManager();

        /* Starts transaction */
        final EntityTransaction transaction1Em1 = em1.getTransaction();
        transaction1Em1.begin();

        LOG.info("-------- UPDATE NEW READONLY ENTITY -----------");
        /* To retrieve an entity I'll use query and 2nd level cache */
        @SuppressWarnings("unchecked")
        final List<ReadOnlyEntity> rewriteEntities = em1.createQuery("select rw from ReadOnlyEntity rw").setHint("org.hibernate.cacheable", true).getResultList();
        final ReadOnlyEntity readWriteEntity = rewriteEntities.get(0);
        readWriteEntity.setName("NEW NAME");

        /*
         * Transaction commit should fail in read-only case.
         */
        try {
            transaction1Em1.commit();
            Assert.fail("A read-only shouldn't be updateable.");
        } catch (final javax.persistence.RollbackException e) {
            LOG.info("The entity is not updateable. " + e.getMessage());
        }

        em1.close();

    }

    /**
     * @param em
     */
    private void countEntities(final EntityManager em, final String emName) {
        final long startTimestamp = new Date().getTime();
        @SuppressWarnings("unchecked")
        final List<ReadOnlyEntity> rewriteEntities = em.createQuery("select rw from ReadOnlyEntity rw").setHint("org.hibernate.cacheable", true).getResultList();
        final long endTimeStamp = new Date().getTime();

        final long time = endTimeStamp - startTimestamp;
        LOG.info(emName + " found " + rewriteEntities.size() + " ReadOnly entities in " + time + " ms");
    }



}
