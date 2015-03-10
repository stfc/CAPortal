/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.ngs.controllers;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for the
 * <code>/denied</code> page
 *
 * @author David Meredith
 */
@Controller
@RequestMapping("/denied")
public class DeniedController {
   
    /**
     * Handle GETs to '/denied' for Idempotent page refreshes.
     */
    @RequestMapping(method = RequestMethod.GET)
    public String handleGetRequest(HttpEntity<String> entity) {
        //return "redirect:/denied/denied";
        return "denied/denied";
    }


    /**
     * Handle GETs to '/denied/denied' to map to 403 page.
     */
    /*@RequestMapping(value="denied", method = RequestMethod.GET)
    public String handleGetRequests(
            HttpEntity<String> entity) {
            //HttpServletResponse response) throws IOException {
        //entity.getHeaders().add("HTTP/1.0 403 Forbidden", null);
        //response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden you are");
        return "denied/denied";
    }*/
}
