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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ngs.forms.VerifyPinFormBean;

import javax.validation.Valid;
import java.security.NoSuchAlgorithmException;

/**
 * Controller for the '/raop/verifyPin' page.
 *
 * @author David Meredith
 */
@Controller
@RequestMapping("/raop/verifyPin")
@Secured("ROLE_RAOP")
public class VerifyPin {
    private static final Log log = LogFactory.getLog(VerifyPin.class);


    @ModelAttribute
    public void populateModel(@RequestParam(required = false) String pin, ModelMap model) {
        // add the form bean for form POSTs
        model.put("verifyPinFormBean", new VerifyPinFormBean());

        // add the optional PIN to the model that comes from a URL request param  
        if (pin != null) {
            model.put("pin", pin);
        }
    }

    /**
     * Handle GET requests to <pre>/raop/verifypin</pre>.
     *
     * @return 'raop/verifyPin'
     */
    @RequestMapping(method = RequestMethod.GET)
    public String handleGetRequest(ModelMap model, RedirectAttributes redirectAttributes) {

        // populateModel() adds pin to model from the optional RequestParam GET param 
        if (model.get("pin") != null) {
            redirectAttributes.addAttribute("pin", model.get("pin"));
            log.debug("pin in get: " + model.get("pin"));
        }
        return "raop/verifyPin";
    }


    /**
     * Handle POSTs to <pre>/raop/verifypin</pre>.
     *
     * @return 'raop/verifyPin' on error or 'redirect:/raop/verifyPin' on success
     */
    @RequestMapping(method = RequestMethod.POST)
    public String handlePost(
            @Valid VerifyPinFormBean verifyPinFormBean, BindingResult result,
            ModelMap model, RedirectAttributes redirectAttributes) throws NoSuchAlgorithmException {

        if (result.hasErrors()) {
            return "raop/verifyPin";
        }
        String pinHash = verifyPinFormBean.getPin();
        String pinPlain = verifyPinFormBean.getPinVerification();

        // Try and recreate the pinHash using SHA-1 
        boolean verifiedOK = DigestUtils.sha1Hex(pinPlain).equalsIgnoreCase(pinHash);

        // First do a regular sha1hex hash 
        // if it did not verify, try appending null char and the string 'exit'
        // followed by newline which simulates a known OpenCA bug (OpenCA 
        // erroneously appends these extra chars when calculating the original hash)
        if (!verifiedOK && DigestUtils.sha1Hex(pinPlain + "\u0000exit\n").equalsIgnoreCase(pinHash)) {
            verifiedOK = true;
        }

        if (verifiedOK) {
            redirectAttributes.addFlashAttribute("verifiedOk", "Pin Verified OK");
        } else {
            redirectAttributes.addFlashAttribute("notVerifiedOk", "Pin Did Not Verify");
        }

        redirectAttributes.addAttribute("pin", verifyPinFormBean.getPin());
        return "redirect:/raop/verifyPin";
    }


    /*private String getHash(String originalValue) {
        try {
            java.security.MessageDigest d = null;
            d = java.security.MessageDigest.getInstance("SHA-1");
            //d = java.security.MessageDigest.getInstance("MD5");
            d.reset();
            d.update(originalValue.getBytes("UTF-8"));  //originally was: d.update(originalValue.getBytes());
            byte[] b = d.digest();

            StringBuffer sb = new StringBuffer(b.length * 2);
            for (int i = 0; i < b.length; i++) {
                int v = b[i] & 0xff;
                if (v < 16) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(v));
            }
            return sb.toString().toUpperCase();
        } catch (Exception ep) {
            ep.printStackTrace();
            return null;
        }
    }*/

}
