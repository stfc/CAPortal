/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.ngs.forms;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author djm76
 */
public class EditBulkForm implements Serializable {
  private List<RequestRowViewWrapper> requestRows; 

    /**
     * @return the requestRows
     */
    public List<RequestRowViewWrapper> getRequestRows() {
        return requestRows;
    }

    /**
     * @param requestRows the requestRows to set
     */
    public void setRequestRows(List<RequestRowViewWrapper> requestRows) {
        this.requestRows = requestRows;
    }
  
}
