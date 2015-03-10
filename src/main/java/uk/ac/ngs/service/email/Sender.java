/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.ngs.service.email;

import java.util.Map;
import org.springframework.mail.SimpleMailMessage;

/**
 * Sends and email message. 
 * @author David Meredith
 */
public interface Sender {
    
   /**
     * Sends e-mail using a named template file (e.g. Velocity template) for the 
     * body and the properties passed in as variables.
     *
     * @param msg                 The e-mail message to be sent, except for the body.
     * @param hTemplateVariables  Variables to use when processing the template.
     * @param templateFileName The name of the template file in which to bind variables. 
     */
    public void send(SimpleMailMessage msg, Map<String, Object> hTemplateVariables, String templateFileName); 
}
