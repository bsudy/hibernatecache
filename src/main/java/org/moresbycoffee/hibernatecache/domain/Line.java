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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Database entity to represent a railway line.
 *
 * @author Barnabas Sudy (barnabas.sudy@gmail.com)
 * @since 2012
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Line {

    /** Unique identifier. <tt>null</tt> until it's persisted. */
    @Id
    @GeneratedValue
    private Long id;

    /** The name of the line. (NonNull) */
    private String name;

    /** The list of the stations on the line. (NonNull) */
    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Station> stations = new ArrayList<Station>();

    /** Default constructor for hibernate. */
    Line() { /* NOP */ }

    /**
     * @param name The name of the line. (NonNull)
     */
    public Line(final String name) {
        super();
        this.name = name;
    }

    /**
     * @return Unique identifier. <tt>null</tt> until it's persisted.
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id The unique identifier to set. (package private to avoid modification.)
     */
    void setId(final Long id) {
        this.id = id;
    }

    /**
     * @return The name of the line. (NonNull)
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name of the line to set. (NonNull)
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the stations The list of the stations on the line. (NonNull)
     */
    public List<Station> getStations() {
        return stations;
    }

    /**
     * @param stations The list of the stations on the line to set. (NonNull)
     */
    void setStations(final List<Station> stations) {
        this.stations = stations;
    }






}
