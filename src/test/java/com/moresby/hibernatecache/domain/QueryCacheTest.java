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

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * TODO javadoc.
 *
 * @author Barnabas Sudy (barnabas.sudy@gmail.com)
 * @since 2012
 */
public class QueryCacheTest extends EntityManagerTest {

    /** Logger. */
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(QueryCacheTest.class);

    @Test
    public void withoutSecondLevelCacheTest() {
        final EntityManager em1 = emf.createEntityManager();
        //Number of the insert statements
        final List<NoCacheEntity> entitiesNoChacheEm1 = getNoCacheEntities(em1, "EM1");

        printStat(em1, "EM1");
        /* Entities haven't been added to 2nd level cache. */
        assertStat(em1, 1, 0, 0, 0);
        //One select statement added
        em1.close();

        final EntityManager em2 = emf.createEntityManager();
        final List<NoCacheEntity> entitiesNoChacheEm2 = getNoCacheEntities(em2, "EM1");

        printStat(em2, "EM2");
        /* Entities haven't been added to 2nd level cache. */
        assertStat(em2, 90, 1, 0, 0);

        assertEquals(entitiesNoChacheEm1.size(), entitiesNoChacheEm2.size());
        em2.close();

    }



}
