package uk.ac.ngs.forms;

import java.io.Serializable;
//import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
//import javax.validation.constraints.NotNull;
//import javax.validation.constraints.Pattern;


/**
 * Simple form submission bean for submitting a page number. 
 * @author David Meredith 
 */
public class GotoPageNumberFormBean  implements Serializable {
    
   
    @Min(value=0, message="0 is minimum")
    private Integer gotoPageNumber = 0;

    /**
     * @return the pageNumber
     */
    public Integer getGotoPageNumber() {
        return gotoPageNumber;
    }

    /**
     * @param pageNumber the pageNumber to set
     */
    public void setGotoPageNumber(Integer gotoPageNumber) {
        this.gotoPageNumber = gotoPageNumber;
    }
    
    
}
