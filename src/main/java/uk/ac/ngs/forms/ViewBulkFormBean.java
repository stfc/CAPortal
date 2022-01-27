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

package uk.ac.ngs.forms;

import uk.ac.ngs.domain.RequestWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Form bean for the viewbulk page.
 *
 * @author Sam Worley
 * @author David Meredith
 */
public class ViewBulkFormBean {

    private List<RequestWrapper> rows = new ArrayList<>(0);
    private Long bulkId;

    public ViewBulkFormBean() {
    }

    public List<RequestWrapper> getRows() {
        return rows;
    }

    public void setRows(List<RequestWrapper> reqRows) {
        this.rows = reqRows;
    }

    /**
     * @return the bulkId
     */
    public Long getBulkId() {
        return bulkId;
    }

    /**
     * @param bulkId the bulkId to set
     */
    public void setBulkId(Long bulkId) {
        this.bulkId = bulkId;
    }
}
