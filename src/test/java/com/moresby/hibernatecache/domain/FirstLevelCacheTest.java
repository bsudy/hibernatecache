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

import static org.junit.Assert.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

/**
 * TODO javadoc.
 *
 * @author Barnabas Sudy (barnabas.sudy@gmail.com)
 * @since 2012
 */
public class FirstLevelCacheTest extends EntityManagerTest {

    /** Logger. */
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(FirstLevelCacheTest.class);

    private EntityManager em1;

    @Before
    public void testing() {
        em1 = emf.createEntityManager();
    }

    /**
     * The same record in the database should be populated into the same entity in one session.
     */
    @Test
    public void alwaysTheSameInSession() {

        final EntityTransaction transaction = em1.getTransaction();
        try {
            transaction.begin();

            final ReadWriteEntity newEntity = new ReadWriteEntity("NEW ENTITY");
            em1.persist(newEntity);

            transaction.commit();

            assertEquals(newEntity, em1.find(ReadWriteEntity.class, newEntity.getId()));

            /* If you clear the 1st level cache than you loose the references. */
//            em1.clear();
//            assertEquals(newEntity, em1.find(ReadWriteEntity.class, newEntity.getId()));
        } finally {
            em1.close();
        }
    }

    /**
     * The instantiated and persisted entity should became part of the session.
     */
    @Test
    public void keepReferenceOfTheNew() {
        final EntityTransaction transaction = em1.getTransaction();
        try {
            transaction.begin();

            @SuppressWarnings("unchecked")
            final List<ReadWriteEntity> readWriteEntityList1 = em1.createQuery("select rw from ReadWriteEntity rw").getResultList();

            @SuppressWarnings("unchecked")
            final List<ReadWriteEntity> readWriteEntityList2 = em1.createQuery("select rw from ReadWriteEntity rw").getResultList();

            assertTrue(readWriteEntityList1.equals(readWriteEntityList2));
            transaction.rollback();
        } finally {
            em1.close();
        }

    }

    /**
     * The same record in different sessions should be populated into different instances.
     */
    @Test
    public void differentInDifferentSessions() {
        final EntityManager em2 = emf.createEntityManager();
        try {
            final EntityTransaction transaction = em1.getTransaction();

            transaction.begin();

            final ReadWriteEntity newEntity = new ReadWriteEntity("NEW ENTITY");
            em1.persist(newEntity);

            transaction.commit();

            assertEquals(newEntity, em1.find(ReadWriteEntity.class, newEntity.getId()));

            final ReadWriteEntity newEntityInEm2 = em2.find(ReadWriteEntity.class, newEntity.getId());
            assertNotNull(newEntityInEm2);
            assertNotSame(newEntity, newEntityInEm2);

        } finally {
            em1.close();
        }


    }

}
