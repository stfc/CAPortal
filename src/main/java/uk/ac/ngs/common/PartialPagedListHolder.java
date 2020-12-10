/*
 * Copyright (C) 2015 STFC
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ngs.common;

import org.springframework.beans.support.MutableSortDefinition;
import org.springframework.beans.support.SortDefinition;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A bean to hold a partial list of rows.
 *
 * @author David Meredith
 */
public class PartialPagedListHolder<E> implements Serializable {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int DEFAULT_MAX_LINKED_PAGES = 10;
    private List<E> source;
    private Date refreshDate;
    private int totalRows = 0;
    private int row = 0;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private SortDefinition sort;
    private SortDefinition sortUsed;

    /**
     * Create a new holder instance. You'll need to set a source list to be able
     * to use the holder.
     *
     * @see #setSource
     */
    public PartialPagedListHolder() {
        this(new ArrayList<E>(0));
    }

    /**
     * Create a new holder instance with the given source list, starting with a
     * default sort definition (with "toggleAscendingOnProperty" activated).
     *
     * @param source the source List
     * @see MutableSortDefinition#setToggleAscendingOnProperty
     */
    public PartialPagedListHolder(List<E> source) {
        this(source, new MutableSortDefinition(true));
        setTotalRows(source.size());
    }

    public PartialPagedListHolder(List<E> source, int totalRows) {
        this(source, new MutableSortDefinition(true));
        setTotalRows(totalRows);
    }

    /**
     * Create a new holder instance with the given source list.
     *
     * @param source the source List
     * @param sort   the SortDefinition to start with
     */
    public PartialPagedListHolder(List<E> source, SortDefinition sort) {
        setSource(source);
        setSort(sort);
    }

    /**
     * Create a new holder instance with the given source list.
     *
     * @param source the source List
     * @param sort   the SortDefinition to start with
     */
    public PartialPagedListHolder(List<E> source, int totalRows, SortDefinition sort) {
        setSource(source);
        setTotalRows(totalRows);
        setSort(sort);
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getTotalRows() {
        return this.totalRows;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getRow() {
        return this.row;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Set the source list for this holder.
     */
    public void setSource(List<E> source) {
        Assert.notNull(source, "Source List must not be null");
        this.source = source;
        this.refreshDate = new Date();
        this.sortUsed = null;
    }

    /**
     * Return the source list for this holder.
     */
    public List<E> getSource() {
        return this.source;
    }

    /**
     * Return the last time the list has been fetched from the source provider.
     */
    public Date getRefreshDate() {
        return this.refreshDate;
    }

    /**
     * Set the sort definition for this holder. Typically an instance of
     * MutableSortDefinition.
     *
     * @see org.springframework.beans.support.MutableSortDefinition
     */
    public void setSort(SortDefinition sort) {
        this.sort = sort;
    }

    /**
     * Return the sort definition for this holder.
     */
    public SortDefinition getSort() {
        return this.sort;
    }
}
