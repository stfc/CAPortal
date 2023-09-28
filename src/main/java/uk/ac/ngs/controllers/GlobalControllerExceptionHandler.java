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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ngs.common.MutableConfigParams;
import uk.ac.ngs.service.email.EmailService;

import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Controller to handle all exceptions that have not been handled already.
 *
 * @author David Meredith
 */
@ControllerAdvice
public class GlobalControllerExceptionHandler {
    public static final String DEFAULT_ERROR_VIEW = "pub/error/error";
    private static final Log log = LogFactory.getLog(GlobalControllerExceptionHandler.class);
    private EmailService emailService;
    private MutableConfigParams mutableConfigParams;


    @ExceptionHandler(value = AccessDeniedException.class)
    public ModelAndView accessDeniedErrorHandler(HttpServletRequest req, Exception e) throws Exception {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("/denied/denied");
        return mav;
    }

    @Inject
    public void setMutableConfigParams(MutableConfigParams mutableConfigParams) {
        this.mutableConfigParams = mutableConfigParams;
    }

}
