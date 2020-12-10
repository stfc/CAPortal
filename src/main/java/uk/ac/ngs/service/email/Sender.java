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

package uk.ac.ngs.service.email;

import org.springframework.mail.SimpleMailMessage;

import java.util.Map;

/**
 * Sends and email message.
 *
 * @author David Meredith
 */
public interface Sender {

    /**
     * Sends e-mail using a named template file (e.g. Velocity template) for the
     * body and the properties passed in as variables.
     *
     * @param msg                The e-mail message to be sent, except for the body.
     * @param hTemplateVariables Variables to use when processing the template.
     * @param templateFileName   The name of the template file in which to bind variables.
     */
    void send(SimpleMailMessage msg, Map<String, Object> hTemplateVariables, String templateFileName);
}
