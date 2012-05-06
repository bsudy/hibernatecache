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
import java.util.Formatter;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.hibernate.cache.spi.Region;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionImpl;
import org.hibernate.stat.Statistics;
import org.junit.Assert;
import org.junit.Before;

/**
 * Common functionalities for the hibernate cache tests.
 *
 * @author Barnabas Sudy (barnabas.sudy@gmail.com)
 * @since 2012
 */
public abstract class EntityManagerTest {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(EntityManagerTest.class);

    protected EntityManagerFactory emf;

    /**
     * Prepares the persistence context (named: <i>cached</i>).
     * @throws Exception
     */
    @Before
    @SuppressWarnings("deprecation")
    public void prepareTest() throws Exception {

        emf = Persistence.createEntityManagerFactory("cached");
        generateDatabase();

        //Clears 2nd level and query caches
        emf.getCache().evictAll();
        ((org.hibernate.ejb.EntityManagerFactoryImpl) emf).getSessionFactory().evictQueries();

    }


    /**
     * @param em
     */
    protected <T> List<T> getEntities(final EntityManager em, final Class<T> type, final String emName) {
        final CriteriaQuery<T> criteria = em.getCriteriaBuilder().createQuery(type);
        final Root<T> root = criteria.from(type);
        criteria.select(root);

        final long startTimestamp = new Date().getTime();

        final List<T> entities = em.createQuery(criteria).setHint("org.hibernate.cacheable", true).getResultList();
        final long endTimeStamp = new Date().getTime();

        final long time = endTimeStamp - startTimestamp;
        LOG.info(emName + " found " + entities.size() + " " + type.getName() + " entities in " + time + " ms");
        return entities;
    }

    /**
     * @param em
     */
    protected List<ReadOnlyEntity> getROEntities(final EntityManager em, final String emName) {
        return getEntities(em, ReadOnlyEntity.class, emName);
    }

    /**
     * @param em
     */
    protected List<NoCacheEntity> getNoCacheEntities(final EntityManager em, final String emName) {
        return getEntities(em, NoCacheEntity.class, emName);
    }

    /**
     * Generated 90 {@link ReadWriteEntity}s and 90 {@link ReadOnlyEntity}s.
     * @throws Exception If any error happen.
     */
    protected final void generateDatabase() throws Exception {
        final EntityManager em1 = emf.createEntityManager();

        try {
            final EntityTransaction transaction = em1.getTransaction();
            transaction.begin();
            try {
                for (int j = 0; j < 10; j++) {
                    for (int i = 1; i < 10; i++) {
                        em1.persist(new ReadWriteEntity("readWrite" + j));
                    }
                }

                for (int j = 0; j < 10; j++) {
                    for (int i = 1; i < 10; i++) {
                        em1.persist(new ReadOnlyEntity("readOnly" + j));
                    }
                }

                for (int j = 0; j < 10; j++) {
                    for (int i = 1; i < 10; i++) {
                        em1.persist(new NoCacheEntity("noCache" + j));
                    }
                }

                for (int j = 0; j < 10; j++) {
                    for (int i = 1; i < 10; i++) {
                        em1.persist(new NoStrictEntity("noStrict" + j));
                    }
                }
            } catch (final Exception e) {
                transaction.rollback();
                throw e;
            } finally {
                if (transaction.isActive()) {
                    transaction.commit();
                }
                initStat(em1);
            }
        } finally {
            em1.close();
        }
    }

    protected final Statistics getStatistics(final EntityManager em) {
        final SessionFactoryImplementor sessionFactory = ((SessionImpl) em.getDelegate()).getSessionFactory();
        return sessionFactory.getStatistics();
    }

    private long stmtCount = 0;
    private long queryHit;
    private long l2Hit;
    private long l2Put;

    protected final void initStat(final EntityManager em) {
        initStat(getStatistics(em));
    }

    protected final void initStat(final Statistics stats) {
        this.stmtCount  = stats.getPrepareStatementCount();
        this.queryHit   = stats.getQueryCacheHitCount();
        this.l2Hit      = stats.getSecondLevelCacheHitCount();
        this.l2Put      = stats.getSecondLevelCachePutCount();

    }

    protected final void assertStat(final EntityManager em, final int stmtCount, final int queryHit, final int l2Hit, final int l2Put) {
        final Statistics stats = getStatistics(em);

        assertLong("query count",           this.stmtCount + stmtCount, stats.getPrepareStatementCount());
        assertLong("query cache hit count", this.queryHit + queryHit,   stats.getQueryCacheHitCount());
        assertLong("2nd level hit count",   this.l2Hit + l2Hit,         stats.getSecondLevelCacheHitCount());
        assertLong("2d level put count",    this.l2Put + l2Put,         stats.getSecondLevelCachePutCount());
        initStat(stats);

    }

    protected final void assertLong(final String fieldName, final long expected, final long result) {
        Assert.assertEquals("The " + fieldName + " should have been " + expected + " but " + result + " found.", expected, result);
    }
    /**
     * @param em
     */
    protected final void printStat(final EntityManager em, final String emName) {
        final SessionFactoryImplementor sessionFactory = ((SessionImpl) em.getDelegate()).getSessionFactory();
        final Statistics stats = sessionFactory.getStatistics();
        LOG.info("EM: " + emName
                 + " Query count: "     + new Formatter().format("%4d", stats.getQueryExecutionCount())
                 + " Query cache hit: " + new Formatter().format("%4d", stats.getQueryCacheHitCount())
                 + " Entity load: "     + new Formatter().format("%4d", stats.getEntityLoadCount())
                 + " 2nd: Hit: "        + new Formatter().format("%4d", stats.getSecondLevelCacheHitCount())
                 + " Miss: "            + new Formatter().format("%4d", stats.getSecondLevelCacheMissCount())
                 + " Put: "             + new Formatter().format("%4d", stats.getSecondLevelCachePutCount()));


        if (LOG.isTraceEnabled()) {
            stats.logSummary();

            @SuppressWarnings("unchecked")
            final Map<String, Region> secondLevelRegions = sessionFactory.getAllSecondLevelCacheRegions();
            for (final Map.Entry<String, Region> entry : secondLevelRegions.entrySet()) {
                LOG.trace("Region: " + entry.getKey() + " Elements: " + (entry.getValue().getElementCountInMemory() + entry.getValue().getElementCountOnDisk()));

            }
        }
    }


}
