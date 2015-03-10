/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.ngs.forms;

import java.io.Serializable;
import uk.ac.ngs.domain.RequestRow;

/**
 *
 * @author djm76
 */
public class RequestRowViewWrapper implements Serializable {
        private final boolean checked; 
        private RequestRow requestRow; 

        public RequestRowViewWrapper(boolean checked, RequestRow requestRow) {
            this.checked = checked;
            this.requestRow = requestRow;
        }

        /**
         * @return the checked
         */
        public boolean getChecked() {
            return checked;
        }

        

        /**
         * @return the requestRow
         */
        public RequestRow getRequestRow() {
            return requestRow;
        } 

    /**
     * @param requestRow the requestRow to set
     */
    public void setRequestRow(RequestRow requestRow) {
        this.requestRow = requestRow;
    }
}
