/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.ngs.domain;

/**
 * Transfer bean for use in view layer. 
 * This bean adds extra properties such as the checked that are not required 
 * by the domain model, but are required by the view layer. 
 * 
 * @author Sam Worley
 * @author David Meredith 
 */
public class RequestWrapper {
        private boolean checked; 
        private RequestRow requestRow; 

        /**
         * Create a new instance 
         * @param checked
         * @param requestRow 
         */
        public RequestWrapper(boolean checked, RequestRow requestRow) {
            this.checked = checked;
            this.requestRow = requestRow;
        }

        /**
         * Default constructor - required so Spring can auto-create/bind instances in view 
         */
        public RequestWrapper(){
        }

        /**
         * @return the checked
         */
        public boolean isChecked() {
            return checked; 
        }

        /**
         * @return the checked
         */
        public boolean getChecked() {
            return checked; 
        }

        /**
         * Set the checked value 
         * @param checked 
         */
        public void setChecked(boolean checked) {
            this.checked = checked; 
        }

        /**
         * @return the requestRow
         */
        public RequestRow getRequestRow() {
            return requestRow;
        }

        /**
         * Set the RequestRow 
         * @param requestRow 
         */
        public void setRequestRow(RequestRow requestRow) {
           this.requestRow = requestRow;
        }
}
