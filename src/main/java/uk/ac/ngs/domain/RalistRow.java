package uk.ac.ngs.domain;

/**
 * Domain object for the
 * <code>ralist</code> table.
 *
 * @author David Meredith
 */
public class RalistRow {

    private Integer ra_id;
    private Integer order_id;
    private String ou;
    private String l;
    private Boolean active; // BIT    

    /**
     * @return the order_id
     */
    public Integer getOrder_id() {
        return order_id;
    }

    /**
     * @param order_id the order_id to set
     */
    public void setOrder_id(Integer order_id) {
        this.order_id = order_id;
    }

    /**
     * @return the ou
     */
    public String getOu() {
        return ou;
    }

    /**
     * @param ou the ou to set
     */
    public void setOu(String ou) {
        this.ou = ou;
    }

    /**
     * @return the l
     */
    public String getL() {
        return l;
    }

    /**
     * @param l the l to set
     */
    public void setL(String l) {
        this.l = l;
    }

    /**
     * @return the active
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * @param active the active to set
     */
    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
     * @return the ra_id
     */
    public Integer getRa_id() {
        return ra_id;
    }

    /**
     * @param ra_id the ra_id to set
     */
    public void setRa_id(Integer ra_id) {
        this.ra_id = ra_id;
    }
}
