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
