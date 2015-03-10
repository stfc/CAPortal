/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.ngs.forms;

import java.util.ArrayList;
import java.util.List;
import uk.ac.ngs.domain.RequestWrapper;

/**
 * Form bean for the viewbulk page. 
 * @author Sam Worley
 * @author David Meredith
 */
public class ViewBulkFormBean {
    
    private List<RequestWrapper> rows = new ArrayList<RequestWrapper>(0);
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
