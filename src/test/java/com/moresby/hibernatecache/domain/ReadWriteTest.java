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

import java.sql.SQLException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import org.apache.log4j.Logger;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * This class can test the behavior of the Hibernate 2nd level cache
 * with {@link CacheConcurrencyStrategy#READ_WRITE read-write} entities.
 *
 * @author Barnabas Sudy (barnabas.sudy@gmail.com)
 * @since 2012
 */
public class ReadWriteTest extends EntityManagerTest {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(ReadWriteTest.class);

    @Test
    public void addReadWriteEntityCacheTest() {
        //Tests the read write cached entities.
        LOG.info("---------------------------------------------------------------------");
        LOG.info("------------------------- READ WRITE TEST ---------------------------");
        LOG.info("---------------------------------------------------------------------");

        final EntityManager em1 = emf.createEntityManager();
        final EntityManager em2 = emf.createEntityManager();

        final SessionFactoryImplementor sessionFactory = ((SessionImpl) em1.getDelegate()).getSessionFactory();
        try {
            LOG.info("Isolation level: " + sessionFactory.getConnectionProvider().getConnection().getTransactionIsolation());
            /*
             * TRANSACTION_READ_UNCOMMITTED = 1
             * TRANSACTION_READ_COMMITTED   = 2
             * TRANSACTION_REPEATABLE_READ  = 4
             * TRANSACTION_SERIALIZABLE     = 8
             */


        } catch (final SQLException e) {
            e.printStackTrace();
        }

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
         * The content of the 2nd level cache is newer than the the transaction,
         * therefore the entities will be pulled out from the database
         * bypassing the cache and deferring the responsibility to the database.
         *
         * The question is why? What is the problem with the data in the 2nd level cache.
         * It's newer than our transaction, it should be okay.
         * Check the cases to understand this behavior.
         *
         * TODO rethink the cases.
         *
         * 1 case: Starts the first transaction, starts the second transaction, the first retrieves the entities modifies them, the second retrieves them ... what should is get?
         * 2 case: Starts the first transaction, the first retrieves the entities and starts the new session and retrieves the entities ... what should it get?
         * Listen, because no commit has occurred in these cases.
         *
         * What is the main difference:
         * 1 case: The second level cache is not in use.
         * 2 case: The second level cache is IN USE.
         *
         * The answer is in the isolation levels / read phenomena. Because Read-write should never-ever be used with Serializable isolation level,
         * because it can't perform the requirements, I'm gonna check the next level the Non repeatable reads problem.
         * When the 2nd entity manager reads the data, it found that the data in the 2nd level cache is newer than its own
         * transaction begin timestamp. If the data had been read earlier (it didn't happen), the hibernate
         * wouldn't be able to decide that the 2nd level cache data is the same that the entity manager had read earlier.
         * To ensure the isolation level Hibernate de.... the request to the database.
         *
         * Query cache: The query will be found in the query cache.
         * 2nd level cache: The entities in 2nd level cache are newer than the transaction so hibernate will retrive a new set.
         */
        LOG.info("--------------------------------------------------------------------------");
        //Query count still 1 - WHY??? - Where are the entities come from?
        countEntities(em2, "EM2");
        printStat(em2, "EM2");
        assertStat(em1, 90, 1, 0, 0);

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
         * the 2nd level cache because this transaction is newer than
         * the content of the 2nd level cache.
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
        LOG.info("-------- INSERT NEW READWRITE ENTITY -----------");
        em1.persist(new ReadWriteEntity("newEntity"));
        em1.flush();
        assertStat(em1, 1, 0, 0, 0); //One insert statement

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

        LOG.info("*** STEP 7");
        /*
         * Here the em2 can't reach the database because the database is locked.
         * The result at this point is database dependent. The isolation level what
         * I'm using is Repeatable Reads isolation level so the h2 database will
         * throw an exception.
         * If you change the isolation level, the test probably will fail.
         *
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

            assertStat(em1, 1, 0, 0, 0);
            em3.close();
        }

        LOG.info("*** STEP 8 - COMMIT");
        transaction1Em1.commit();

        LOG.info("*** STEP 9");
        /*
         * Runs the query on the database and stores the new entity in the 2nd level cache.
         * Query cache: New result added.
         * 2nd level cache: New entity added.
         */
        countEntities(em2, "EM2");
        printStat(em2, "EM2");
        assertStat(em1, 1, 0, 0, 1);


        LOG.info("*** STEP 10");
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

        LOG.info("*** STEP 11");

        /*
         * The query found in the query cache, but the 2nd level cache still not in
         * use because of the timestamps to avoid the Non repeatable read phenomena.
         * Comment: It would use the 1st level cache, therefore I cleared it.
         * Query cache: In use.
         * 2nd level cache: Not in use. (but everything in the 1st level cache.)
         */
        em2.clear();
        countEntities(em2, "EM2");
        printStat(em2, "EM2");
        assertStat(em2, 91, 1, 0, 0);

        em2.close();
        em1.close();

    }

    @Test
    public void updateReadWriteEntityCacheTest() {
        //Tests the read write cached entities.
        LOG.info("---------------------------------------------------------------------");
        LOG.info("------------------------- READ WRITE TEST ---------------------------");
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
         * The content of the 2nd level cache is newer than the the transaction,
         * therefore the entities will be pulled out from the database
         * bypassing the cache and deferring the responsibility to the database.
         * Query cache: The query will be found in the query cache.
         * 2nd level cache: The entities in 2nd level cache are newer than the transaction so hibernate will retrive a new set.
         */
        countEntities(em2, "EM2");
        printStat(em2, "EM2");
        assertStat(em1, 90, 1, 0, 0);

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
         * the 2nd level cache because this transaction is newer than
         * the content of the 2nd level cache.
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
        LOG.info("-------- UPDATE NEW READWRITE ENTITY -----------");
        /* To retrieve an entity I'll use query and 2nd level cache */
        @SuppressWarnings("unchecked")
        final List<ReadWriteEntity> rewriteEntities = em1.createQuery("select rw from ReadWriteEntity rw").setHint("org.hibernate.cacheable", true).getResultList();
        final ReadWriteEntity readWriteEntity = rewriteEntities.get(0);
        readWriteEntity.setName("NEW NAME");
        em1.flush();
        assertStat(em1, 1, 1, 0, 0); //???

        LOG.info("*** STEP 6");
        /*
         * The 2nd level cache is in invalid we added a modified an entry in a table and
         * the hibernate doesn't know what would be the result of the query without
         * the database.
         * Query cache: Not in use
         * 2nd level cache: Not in use
         */
        countEntities(em1, "EM1");
        printStat(em1, "EM1");
        assertStat(em1, 1, 0, 0, 0);

        LOG.info("*** STEP 7");
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
            assertStat(em1, 1, 0, 0, 0);
            em3.close();
        }

        LOG.info("*** STEP 8 - COMMIT");
        transaction1Em1.commit();

        LOG.info("*** STEP 9");
        /*
         * Runs the query on the database and stores the new entity in the 2nd level cache.
         * Query cache: New result added.
         * 2nd level cache: The modified entity added.
         */
        countEntities(em2, "EM2");
        printStat(em2, "EM2");
        assertStat(em1, 1, 0, 0, 1);


        LOG.info("*** STEP 10");
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
            countEntities(em3, "EM3");
            printStat(em3, "EM3");
            assertStat(em1, 0, 1, 90, 0);

            em3.close();
        }

        LOG.info("*** STEP 11");
        /*
         * The query found in the query cache, but the 2nd level cache still not in
         * use because of the timestamps to avoid the Non repeatable read phenomena.
         * Query cache: New result added.
         * 2nd level cache: The modified entity added.
         */
        em2.clear();
        countEntities(em2, "EM2");
        printStat(em2, "EM2");
        assertStat(em1, 90, 1, 0, 0);

        em2.close();
        em1.close();

    }

    /**
     * @param em
     */
    private void countEntities(final EntityManager em, final String emName) {
        getEntities(em, ReadWriteEntity.class, emName);
    }



}
