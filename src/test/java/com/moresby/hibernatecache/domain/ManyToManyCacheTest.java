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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * TODO javadoc.
 *
 * @author Barnabas Sudy (barnabas.sudy@gmail.com)
 * @since 2012
 */
public class ManyToManyCacheTest extends EntityManagerTest {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(ManyToManyCacheTest.class);


    private Station getStationByName(final EntityManager em, final String stationName) {
        final Query query = em.createQuery("from Station where name like :stationName");
        query.setParameter("stationName", stationName);
        return (Station) query.getSingleResult();
    }

    @Test
    public void testLazyFetchFirstLevelCache() {

        final EntityManager em1 = emf.createEntityManager();

        final Station westminster = getStationByName(em1, "Westminster");
        printStat(em1, "EM1");
        assertStat(em1, 1, 0, 0, 1);

        final List<Line> linesAtWestminster = westminster.getLines();
        for (final Line line : linesAtWestminster) {
            LOG.info("Line: " + line.getName());
        }

        printStat(em1, "EM1");
        assertStat(em1, 1, 0, 0, 3); //Why only 1???

        final Station victoria = getStationByName(em1, "Victoria");
        printStat(em1, "EM1");
        assertStat(em1, 1, 0, 0, 1);

        final List<Line> linesAtVictoria = victoria.getLines();
        for (final Line line : linesAtVictoria) {
            LOG.info("Line: " + line.getName());
        }
        printStat(em1, "EM1");
        assertStat(em1, 1, 0, 0, 1); //Why only 1???
    }

}
