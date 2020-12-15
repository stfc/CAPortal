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
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ngs.common.Pair;
import uk.ac.ngs.common.PartialPagedListHolder;
import uk.ac.ngs.dao.JdbcRalistDao;
import uk.ac.ngs.dao.JdbcRequestDao;
import uk.ac.ngs.domain.RalistRow;
import uk.ac.ngs.domain.RequestRow;
import uk.ac.ngs.forms.GotoPageNumberFormBean;
import uk.ac.ngs.forms.SearchCsrFormBean;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertUtil;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.*;

/**
 * Controller for the "/raop/searchcsr" page for CSR searches.
 * <p>
 * The controller handles pagination through CSR domain object results.
 * Searches can be submitted via GET or POST - both do the same validation.
 * On submission of a form POST, posted params are validated and the request
 * is re-directed to the GET handler with the appropriate GET request params
 * for the search. This way, a similar code path is used to execute DB searches
 * (same apart from the GET/POST entry point).
 *
 * @author David Meredith
 */
@Controller
@RequestMapping("/raop/searchcsr")
@SessionAttributes(value = {SearchCSR.SEARCH_CSR_FORM_BEAN_SESSIONSCOPE})
@Secured("ROLE_RAOP")
public class SearchCSR {

    private static final Log log = LogFactory.getLog(SearchCSR.class);
    private JdbcRequestDao jdbcRequestDao;
    private JdbcRalistDao ralistDao;
    private SecurityContextService securityContextService;

    /**
     * Name of the model attribute used to bind form POSTs.
     */
    public static final String SEARCH_CSR_FORM_BEAN_SESSIONSCOPE = "searchCsrFormBean";
    /**
     * Name of model attribute that stores search results in session.
     * Accessed in JSP using <code>${sessionScope.csrSearchPageHolder}</code>
     */
    public static final String CSR_PAGE_LIST_HOLDER_SESSIONSCOPE = "csrSearchPageHolder";
    /**
     * Name of the model attribute that stores the list of RAs.
     */
    public static final String RA_ARRAY_REQUESTSCOPE = "ralistArray";
    /**
     * Name of the model attribute used to bind GET request search parameters.
     */
    public static final String SEARCH_CSR_FORM_BEAN_GET_REQUESTSCOPE = "searchCsrFormBean_REQUESTSCOPE";

    /**
     * ModelAttribute annotations defined on a method in a controller are
     * invoked before RequestMapping methods, within the same controller.
     *
     * @param model
     * @param session
     */
    @ModelAttribute
    public void populateModel(Model model, HttpSession session) {
        // Populate model with an empty list if session var is not present (e.g. session expired) 
        PartialPagedListHolder<RequestRow> pagedListHolder =
                (PartialPagedListHolder<RequestRow>) session.getAttribute(SearchCSR.CSR_PAGE_LIST_HOLDER_SESSIONSCOPE);
        if (pagedListHolder == null) {
            pagedListHolder = new PartialPagedListHolder<RequestRow>(new ArrayList<RequestRow>(0));
            session.setAttribute(SearchCSR.CSR_PAGE_LIST_HOLDER_SESSIONSCOPE, pagedListHolder);
        }
        // Populate the RA list pull down 
        List<RalistRow> rows = this.ralistDao.findAllByActive(true, null, null);
        List<String> raArray = new ArrayList<String>();
        String userDN = this.securityContextService.getCaUserDetails().getCertificateRow().getDn();
        String l = CertUtil.extractDnAttribute(userDN, CertUtil.DNAttributeType.L);
        String ou = CertUtil.extractDnAttribute(userDN, CertUtil.DNAttributeType.OU);

        // add user's RA as first option 
        if (l != null && ou != null) {
            // BUG - have had trouble submitting RA values that contain whitespace, 
            // so have replaced whitespace in ra with underscore 
            raArray.add(ou + "_" + l);
        }
        // then add the 'all' option second 
        raArray.add("all");
        // then add all other RAs
        for (RalistRow row : rows) {
            // BUG - have had trouble submitting RA values that contain whitespace, 
            // so have replaced whitespace in ra with underscore 
            String raval = row.getOu() + "_" + row.getL();
            raArray.add(raval);
        }
        model.addAttribute(RA_ARRAY_REQUESTSCOPE, raArray.toArray());
    }

    /**
     * Invoked initially to add the 'searchCsrFormBean' model attribute. Once
     * created the "searchCsrFormBean" comes from the HTTP session (see
     * SessionAttributes annotation)
     */
    @ModelAttribute(SEARCH_CSR_FORM_BEAN_SESSIONSCOPE)
    public SearchCsrFormBean createFormBean() {
        return new SearchCsrFormBean();
    }

    @ModelAttribute(SEARCH_CSR_FORM_BEAN_GET_REQUESTSCOPE)
    public SearchCsrFormBean createGetRequestFormBean() {
        return new SearchCsrFormBean();
    }

    /**
     * Add the 'gotoPageFormBean' to the model if not present.
     */
    @ModelAttribute("gotoPageFormBean")
    public GotoPageNumberFormBean createGotoFormBean() {
        return new GotoPageNumberFormBean();
    }

    /**
     * Handle GET requests to <pre>/raop/searchcsr</pre> for Idempotent page refreshes.
     *
     * @return
     */
    @RequestMapping(method = RequestMethod.GET)
    public String handleGetRequest() {
        return "raop/searchcsr";
    }


    /**
     * Handle POSTs to "/raop/searchcsr" to do a CSR search.
     * The method processes the form input and then redirects to the "/search"
     * URL (handled in this controller) with URL encoded GET params that define
     * the search parameters.
     *
     * @return "redirect:/raop/searchcsr/search?multiple=search&params=go here"
     */
    /*@RequestMapping(method = RequestMethod.POST)
    public String submitSearch(
            @Valid SearchCsrFormBean searchCsrFormBean, BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {

        if (result.hasErrors()) {
            log.warn("binding and validation errors");
            return "raop/searchcsr";
        }

        searchCsrFormBean.setStartRow(0); 
        log.debug("adding message search submitted ok: ["+searchCsrFormBean.getRa()+"]"); 

        // Store a success message for rendering on the next request after redirect
        redirectAttrs.addFlashAttribute("message", "Search Submitted OK");
        
        
        // Now submit to our GET /search handler to run the query 
        //String url = response.encodeRedirectURL("/raop/searchcsr/search?"+this.buildGetRequestParams(searchCsrFormBean)); 
        // http://forum.springsource.org/showthread.php?86633-Spring-3-annotated-controllers-redirection-strings-and-appending-parameters-to-URL
        String url = "/raop/searchcsr/search?"+this.buildGetRequestParams(searchCsrFormBean); 
        return "redirect:"+url; 
    }*/
    @RequestMapping(method = RequestMethod.POST)
    public String submitSearch(
            @Valid SearchCsrFormBean searchCsrFormBean, BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {

        if (result.hasErrors()) {
            log.warn("binding and validation errors");
            return "raop/searchcsr";
        }

        searchCsrFormBean.setStartRow(0);
        log.debug("adding message search submitted ok: [" + searchCsrFormBean.getRa() + "]");

        // Now submit to our GET /search handler to run the query 
        // http://forum.springsource.org/showthread.php?86633-Spring-3-annotated-controllers-redirection-strings-and-appending-parameters-to-URL
        //String url = "/raop/searchcsr/search?"+this.buildGetRequestParams(searchCsrFormBean); 
        redirectAttrs.addAllAttributes(this.getParams(searchCsrFormBean));
        // Store a success message for rendering on the next request after redirect
        //redirectAttrs.addFlashAttribute("message", "Search Submitted OK");

        return "redirect:/raop/searchcsr/search";
    }

    /*@RequestMapping(method = RequestMethod.POST)
    public ModelAndView submitSearch(
            @Valid SearchCsrFormBean searchCsrFormBean, BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            //Model model,
            HttpSession session,
            //HttpServletResponse response
            ) {

        if (result.hasErrors()) {
            log.warn("binding and validation errors");
            return new ModelAndView("raop/searchcsr"); // "raop/searchcsr";
        }

        searchCsrFormBean.setStartRow(0); 
        log.debug("adding message search submitted ok: ["+searchCsrFormBean.getRa()+"]"); 

        // Store a success message for rendering on the next request after redirect
        redirectAttrs.addFlashAttribute("message", "Search Submitted OK");
        
        
        // Now submit to our GET /search handler to run the query 
        //String url = response.encodeRedirectURL("/raop/searchcsr/search?"+this.buildGetRequestParams(searchCsrFormBean)); 
        //String url = "/raop/searchcsr/search?"+this.buildGetRequestParams(searchCsrFormBean); 
        //return "redirect:"+url; 
        ModelAndView mav = new ModelAndView();
        mav.setView(new RedirectView("/raop/searchcsr/search", true, true, true)); 
        this.populateParams(mav, searchCsrFormBean);  
        return mav; 
    }*/

    private Map<String, Object> getParams(SearchCsrFormBean searchCsrFormBean) {
        Map<String, Object> mav = new HashMap<String, Object>();
        if (StringUtils.hasText(searchCsrFormBean.getName())) {
            mav.put("name", searchCsrFormBean.getName());
        }
        if (StringUtils.hasText(searchCsrFormBean.getDn())) {
            mav.put("dn", searchCsrFormBean.getDn());
        }
        if (StringUtils.hasText(searchCsrFormBean.getData())) {
            mav.put("data", searchCsrFormBean.getData());
        }
        if (StringUtils.hasText(searchCsrFormBean.getEmailAddress())) {
            mav.put("emailAddress", searchCsrFormBean.getEmailAddress());
        }
        if (searchCsrFormBean.getSearchNullEmailAddress() != null) {
            mav.put("searchNullEmailAddress", searchCsrFormBean.getSearchNullEmailAddress().toString());
        }
        if (searchCsrFormBean.getReq_key() != null) {
            mav.put("req_key", searchCsrFormBean.getReq_key().toString());
        }
        if (StringUtils.hasText(searchCsrFormBean.getRa())) {
            mav.put("ra", searchCsrFormBean.getRa());
        }
        if (StringUtils.hasText(searchCsrFormBean.getStatus())) {
            mav.put("status", searchCsrFormBean.getStatus());
        }
        if (searchCsrFormBean.getStartRow() != null) {
            mav.put("startRow", searchCsrFormBean.getStartRow());
        }
        if (searchCsrFormBean.getShowRowCount() != null) {
            mav.put("showRowCount", searchCsrFormBean.getShowRowCount());
        }
        return mav;
    }


    /**
     * Handle GETs to "/raop/searchcsr/search?search=params&for=dbquery"
     * to perform a DB search.
     * <p>
     * Processes the known GET params and submits a query to the DB. Unknown
     * GET request parameters are ignored. Binding/validation of GET params
     * is done via Spring @ModelAttribute binding with the given SearchCsrFormBean.
     * If only a partial query is specified, e.g. "/search?name=some cn",
     * default values as defined in SearchCsrFormBean are applied. After successful
     * binding/validation/query, the SEARCH_CSR_FORM_BEAN_SESSIONSCOPE model attribute
     * is updated.
     *
     * @return "raop/searchcsr"
     */
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String submitSearchViaGet(
            @Valid @ModelAttribute(SEARCH_CSR_FORM_BEAN_GET_REQUESTSCOPE)
                    SearchCsrFormBean searchCsrFormBeanGetReq, BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {

        if (result.hasErrors()) {
            log.warn("binding and validation errors on submitSearchViaGet");
            return "raop/searchcsr";
        }

        // Update our session-scoped @ModelAttribute form bean (SEARCH_CSR_FORM_BEAN_SESSIONSCOPE) 
        // with the request-scoped @ModelAttribute get-request form bean 
        // (SEARCH_CSR_FORM_BEAN_GET_REQUESTSCOPE),  then run the DB search. 
        model.addAttribute(SEARCH_CSR_FORM_BEAN_SESSIONSCOPE, searchCsrFormBeanGetReq);
        this.runSearchUpdateResultsInSession(searchCsrFormBeanGetReq, session);
        log.debug("ra value in session: [" + searchCsrFormBeanGetReq.getRa() + "]");
        model.addAttribute("searchOk", "Search Submitted/Refreshed OK");
        return "raop/searchcsr";
    }

    /**
     * Use the given SearchCsrFormBean to submit the DB search and put the results
     * in session under the CSR_PAGE_LIST_HOLDER_SESSIONSCOPE attribute.
     */
    private void runSearchUpdateResultsInSession(SearchCsrFormBean searchCsrFormBean, HttpSession session) {
        session.setAttribute("lastCSRSearchDate_session", new Date());
        int limit = searchCsrFormBean.getShowRowCount();
        int startRow = searchCsrFormBean.getStartRow();
        Pair<List<RequestRow>, Integer> pair = this.submitCsrSearch(searchCsrFormBean, limit, startRow);
        List<RequestRow> rows = pair.first;
        int totalRows = pair.second;

        PartialPagedListHolder<RequestRow> pagedListHolder = new PartialPagedListHolder<RequestRow>(rows, totalRows);
        pagedListHolder.setPageSize(limit);
        pagedListHolder.setRow(startRow);
        session.setAttribute(SearchCSR.CSR_PAGE_LIST_HOLDER_SESSIONSCOPE, pagedListHolder);
    }

     /*private String buildGetRequestParams(SearchCsrFormBean searchCsrFormBean){
         //try {
            //String enc = "UTF-8";
            StringBuilder params = new StringBuilder("");
            if (StringUtils.hasText(searchCsrFormBean.getName())) {
                params.append("name=");
                //params.append(URLEncoder.encode(searchCsrFormBean.getName(), enc));
                params.append(searchCsrFormBean.getName());
                params.append("&");
            }
            if (StringUtils.hasText(searchCsrFormBean.getDn())) {
                params.append("dn=");
                //params.append(URLEncoder.encode(searchCsrFormBean.getDn(), enc));
                params.append(searchCsrFormBean.getDn());
                params.append("&");
            }
            if (StringUtils.hasText(searchCsrFormBean.getData())) {
                params.append("data=");
                //params.append(URLEncoder.encode(searchCsrFormBean.getData(), enc));
                params.append(searchCsrFormBean.getData());
                params.append("&");
            }
            if (StringUtils.hasText(searchCsrFormBean.getEmailAddress())) {
                params.append("emailAddress=");
                //params.append(URLEncoder.encode(searchCsrFormBean.getEmailAddress(), enc));
                params.append(searchCsrFormBean.getEmailAddress());
                params.append("&");
            } 
            if(searchCsrFormBean.getSearchNullEmailAddress() != null){
                params.append("searchNullEmailAddress=");
                //params.append(URLEncoder.encode(searchCsrFormBean.getSearchNullEmailAddress().toString(), enc));
                params.append(searchCsrFormBean.getSearchNullEmailAddress().toString());
                params.append("&");
            }
            if(searchCsrFormBean.getReq_key() != null){ 
                params.append("req_key=");
                //params.append(URLEncoder.encode(searchCsrFormBean.getReq_key().toString(), enc));
                params.append(searchCsrFormBean.getReq_key().toString());
                params.append("&");
            }
            if(StringUtils.hasText(searchCsrFormBean.getRa())){
                params.append("ra="); 
                //params.append(URLEncoder.encode(searchCsrFormBean.getRa(), enc));
                params.append(searchCsrFormBean.getRa());
                params.append("&"); 
            }
            if(StringUtils.hasText(searchCsrFormBean.getStatus())){
                params.append("status="); 
                //params.append(URLEncoder.encode(searchCsrFormBean.getStatus().toString(), enc));
                params.append(searchCsrFormBean.getStatus().toString());
                params.append("&"); 
            }
            if(searchCsrFormBean.getStartRow() != null){ 
                params.append("startRow=");
                //params.append(URLEncoder.encode(searchCsrFormBean.getStartRow().toString(), enc));
                params.append(searchCsrFormBean.getStartRow().toString());
                params.append("&");
            }
            if(searchCsrFormBean.getShowRowCount() != null){
                params.append("showRowCount=");
                //params.append(URLEncoder.encode(searchCsrFormBean.getShowRowCount().toString(), enc));
                params.append(searchCsrFormBean.getShowRowCount().toString());
                params.append("&");
            }
            
            // remove final & if present 
            String getParams = params.toString();
            if (getParams.endsWith("&")) {
                getParams = getParams.substring(0, getParams.length() - 1);
            }
            return getParams;
        //} catch (UnsupportedEncodingException ex) {
        //    throw new IllegalStateException(ex);
        //}
     }*/

    /**
     * Handle POSTs to "/raop/searchcsr/goto" to paginate to a specific row.
     *
     * @return "redirect:/raop/searchcsr/search?search=params&for=dbquery"
     */
    @RequestMapping(value = "/goto", method = RequestMethod.POST)
    public String submitGotoPage(
            @Valid @ModelAttribute("gotoPageFormBean") GotoPageNumberFormBean gotoPageFormBean,
            BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {

        log.debug("submitGotoPage");
        if (result.hasErrors()) {
            log.warn("submitGotoPage has binding and validation errors");
            return "raop/searchcsr";
        }
        // Get the search data from session. 
        SearchCsrFormBean sessionSearchCsrFormBean =
                (SearchCsrFormBean) session.getAttribute(SearchCSR.SEARCH_CSR_FORM_BEAN_SESSIONSCOPE);
        PartialPagedListHolder<RequestRow> pagedListHolder =
                (PartialPagedListHolder<RequestRow>) session.getAttribute(SearchCSR.CSR_PAGE_LIST_HOLDER_SESSIONSCOPE);

        // update requested start row 
        int startRow = gotoPageFormBean.getGotoPageNumber() - 1; //zero offset when used in SQL  
        //int limit = pagedListHolder.getPageSize();

        // If requested start row is > than total rows or < 0, just return        
        if (startRow > pagedListHolder.getTotalRows() || startRow < 0) {
            return "redirect:/raop/searchcsr";
        }

        // Update requested start row and re-submit the search as a GET request  
        sessionSearchCsrFormBean.setStartRow(startRow);
        redirectAttrs.addAllAttributes(this.getParams(sessionSearchCsrFormBean));
        return "redirect:/raop/searchcsr/search";//+"?"+this.buildGetRequestParams(sessionSearchCsrFormBean);
    }


    /**
     * Handle GETs to "/raop/searchcsr/page" with URL parameters. Used for paging
     * through the csr list when clicking Next,Prev,First,Last links.
     *
     * @return "redirect:/raop/searchcsr/search?multiple=search&params=go here"
     */
    @RequestMapping(value = "page", method = RequestMethod.GET)
    public String handlePageRequest(@RequestParam(required = false) String page,
                                    HttpSession session, RedirectAttributes redirectAttrs) {

        // Get the search data from session. 
        SearchCsrFormBean searchCsrFormBean =
                (SearchCsrFormBean) session.getAttribute(SearchCSR.SEARCH_CSR_FORM_BEAN_SESSIONSCOPE);
        PartialPagedListHolder<RequestRow> pagedListHolder =
                (PartialPagedListHolder<RequestRow>) session.getAttribute(SearchCSR.CSR_PAGE_LIST_HOLDER_SESSIONSCOPE);

        if ("next".equals(page) || "prev".equals(page) || "first".equals(page) || "last".equals(page)) {
            int limit = searchCsrFormBean.getShowRowCount(); //pagedListHolder.getPageSize();
            int startRow = 0;

            if ("next".equals(page)) {
                startRow = pagedListHolder.getRow() + limit;
                if (startRow >= pagedListHolder.getTotalRows()) {
                    startRow = pagedListHolder.getRow();
                }
            }
            if ("prev".equals(page)) {
                startRow = pagedListHolder.getRow() - limit;
                if (startRow < 0) {
                    startRow = 0;
                }
            }
            if ("first".equals(page)) {
                startRow = 0;
            }
            if ("last".equals(page)) {
                startRow = pagedListHolder.getTotalRows() - limit;
                if (startRow > pagedListHolder.getTotalRows()) {
                    startRow = pagedListHolder.getRow();
                }
                if (startRow < 0) {
                    startRow = 0;
                }
            }
            searchCsrFormBean.setStartRow(startRow);
            redirectAttrs.addAllAttributes(this.getParams(searchCsrFormBean));
            return "redirect:/raop/searchcsr/search";//+"?"+this.buildGetRequestParams(searchCsrFormBean); 
        }
        return "redirect:/raop/searchcsr";
    }


    /**
     * Query the DB for a list of request rows and for the total number of
     * rows found that match the search criteria defined by SearchCsrFormBean.
     *
     * @param searchCsrFormBean Used to specify search criteria.
     * @param limit             Limits the number of search results.
     * @param offset            Offset the results by this many rows.
     * @return <code>Pair.first</code> holds the list of request rows (from
     * offset to limit), while <code>Pair.second</code> is the total number of
     * matching rows found in the DB.
     */
    private Pair<List<RequestRow>, Integer> submitCsrSearch(
            SearchCsrFormBean searchCsrFormBean, Integer limit, Integer offset) {

        if (searchCsrFormBean.getReq_key() != null) {
            RequestRow row = this.jdbcRequestDao.findById(searchCsrFormBean.getReq_key());
            List<RequestRow> rows = new ArrayList<RequestRow>(0);
            if (row != null) {
                rows.add(row);
                row.setData(null);
            }
            return Pair.create(rows, rows.size());
        } else {
            // select RA from pulldown and perform search
            Map<JdbcRequestDao.WHERE_PARAMS, String> whereParams = new
                    EnumMap<JdbcRequestDao.WHERE_PARAMS, String>(JdbcRequestDao.WHERE_PARAMS.class);

            String ra = searchCsrFormBean.getRa();
            if (StringUtils.hasText(ra) && !"all".equals(ra)) {
                // BUG - have had trouble submitting RA values that contain whitespace,
                // so have replaced whitespace in ra with underscore - so here revert back
                // to whitespace to do the search. See where the raList is set.
                whereParams.put(JdbcRequestDao.WHERE_PARAMS.RA_EQ, ra.replaceAll("_", " "));
            }
            String status = searchCsrFormBean.getStatus();
            if (StringUtils.hasText(status) && !"all".equals(status)) {
                if ("NEW_or_RENEW".equals(status)) {
                    // The value of status for STATUS_EQ_NEW_or_RENEW is ignored
                    // here, it is actually set as NEW or RENEW manually in the DAO
                    // as a shortcut/hack.
                    whereParams.put(JdbcRequestDao.WHERE_PARAMS.STATUS_EQ_NEW_or_RENEW, status);
                } else {
                    whereParams.put(JdbcRequestDao.WHERE_PARAMS.STATUS_EQ, status);
                }
            }
            String name = searchCsrFormBean.getName();
            if (StringUtils.hasText(name)) {
                whereParams.put(JdbcRequestDao.WHERE_PARAMS.CN_LIKE, this.prepareLikeValue(name));
            }
            String dn = searchCsrFormBean.getDn();
            if (StringUtils.hasText(dn)) {
                whereParams.put(JdbcRequestDao.WHERE_PARAMS.DN_LIKE, this.prepareLikeValue(dn));
            }
            // if search null email address checkbox is true, then we want to search
            // for email values that are null
            if (searchCsrFormBean.getSearchNullEmailAddress()) {
                whereParams.put(JdbcRequestDao.WHERE_PARAMS.EMAIL_EQ, null);
            } else {
                if (StringUtils.hasText(searchCsrFormBean.getEmailAddress())) {
                    whereParams.put(JdbcRequestDao.WHERE_PARAMS.EMAIL_LIKE, this.prepareLikeValue(searchCsrFormBean.getEmailAddress()));
                }
            }
            String data = searchCsrFormBean.getData();
            if (StringUtils.hasText(data)) {
                whereParams.put(JdbcRequestDao.WHERE_PARAMS.DATA_LIKE, this.prepareLikeValue(data));
            }
            List<RequestRow> rows = this.jdbcRequestDao.findBy(whereParams, limit, offset);
            rows = this.jdbcRequestDao.setDataNotBefore(rows);
            int totalRows = jdbcRequestDao.countBy(whereParams);
            // Nullify the Data property before we add the rows to HTTP session to keep
            // the memory requirements low.
            for (RequestRow row : rows) {
                row.setData(null);
            }
            return Pair.create(rows, totalRows);
        }
    }

    private String prepareLikeValue(String value) {
        value = value.trim();
        if (!value.endsWith("%")) {
            value = value + "%";
        }
        if (!value.startsWith("%")) {
            value = "%" + value;
        }
        return value;
    }

    @Inject
    public void setJdbcRequestDao(JdbcRequestDao jdbcRequestDao) {
        this.jdbcRequestDao = jdbcRequestDao;
    }

    @Inject
    public void setRalistDao(JdbcRalistDao ralistDao) {
        this.ralistDao = ralistDao;
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }
}
