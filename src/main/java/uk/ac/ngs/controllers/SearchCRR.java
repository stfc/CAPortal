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
import uk.ac.ngs.dao.JdbcCrrDao;
import uk.ac.ngs.dao.JdbcRalistDao;
import uk.ac.ngs.domain.CrrRow;
import uk.ac.ngs.domain.RalistRow;
import uk.ac.ngs.forms.GotoPageNumberFormBean;
import uk.ac.ngs.forms.SearchCrrFormBean;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertUtil;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.*;

/**
 * Controller for the "/raop/searchcrr" page for CRR searches.
 * <p>
 * The controller handles pagination through CRR domain object results.
 * Searches can be submitted via GET or POST - both do the same validation.
 * On submission of a form POST, posted params are validated and the request
 * is re-directed to the GET handler with the appropriate GET request params
 * for the search. This way, a similar code path is used to execute DB searches
 * (same apart from the GET/POST entry point).
 *
 * @author David Meredith
 */
@Controller
@RequestMapping("/raop/searchcrr")
@SessionAttributes(value = {SearchCRR.SEARCH_CRR_FORM_BEAN_SESSIONSCOPE})
@Secured("ROLE_RAOP")
public class SearchCRR {

    private JdbcCrrDao jdbcCrrDao;
    private JdbcRalistDao ralistDao;
    private SecurityContextService securityContextService;

    private static final Log log = LogFactory.getLog(SearchCRR.class);
    /**
     * Name of the model attribute used to bind form POSTs.
     */
    public static final String SEARCH_CRR_FORM_BEAN_SESSIONSCOPE = "searchCrrFormBean";
    /**
     * Name of model attribute that stores search results in session.
     * Accessed in JSP using <code>${sessionScope.csrSearchPageHolder}</code>
     */
    public static final String CRR_PAGE_LIST_HOLDER_SESSIONSCOPE = "crrSearchPageHolder";
    /**
     * Name of the model attribute used to bind GET request search parameters.
     */
    public static final String SEARCH_CRR_FORM_BEAN_GET_REQUESTSCOPE = SEARCH_CRR_FORM_BEAN_SESSIONSCOPE + "_REQUESTSCOPE";
    /**
     * Name of the model attribute that stores the list of RAs.
     */
    public static final String RA_ARRAY_REQUESTSCOPE = "ralistArray";

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
        PartialPagedListHolder<CrrRow> pagedListHolder =
                (PartialPagedListHolder<CrrRow>) session.getAttribute(CRR_PAGE_LIST_HOLDER_SESSIONSCOPE);
        if (pagedListHolder == null) {
            pagedListHolder = new PartialPagedListHolder<CrrRow>(new ArrayList<CrrRow>(0));
            session.setAttribute(CRR_PAGE_LIST_HOLDER_SESSIONSCOPE, pagedListHolder);
        }

        // Populate the RA list pull down 
        List<RalistRow> rows = this.ralistDao.findAllByActive(true, null, null);
        List<String> raArray = new ArrayList<String>(rows.size());
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
            raArray.add(row.getOu() + "_" + row.getL());
        }
        model.addAttribute(RA_ARRAY_REQUESTSCOPE, raArray);
    }

    /**
     * Invoked initially to add the 'searchCrrFormBean' model attribute. Once
     * created the "searchCrrFormBean" comes from the HTTP session (see
     * SessionAttributes annotation)
     */
    @ModelAttribute(SEARCH_CRR_FORM_BEAN_SESSIONSCOPE)
    public SearchCrrFormBean createFormBean() {
        return new SearchCrrFormBean();
    }

    @ModelAttribute(SEARCH_CRR_FORM_BEAN_GET_REQUESTSCOPE)
    public SearchCrrFormBean createGetRequestFormBean() {
        return new SearchCrrFormBean();
    }

    /**
     * Add the 'gotoPageFormBean' to the model if not present.
     */
    @ModelAttribute("gotoPageFormBean")
    public GotoPageNumberFormBean createGotoFormBean() {
        return new GotoPageNumberFormBean();
    }

    /**
     * Handle GETs to <pre>/raop/searchcrr</pre> for Idempotent page refreshes.
     *
     * @return
     */
    @RequestMapping(method = RequestMethod.GET)
    public String handleGetRequest() {
        return "raop/searchcrr";
    }

    /**
     * Handle POSTs to "/raop/searchcrr" to do a CRR search.
     * The method processes the form input and then redirects to the "/search"
     * URL (handled in this controller) with URL encoded GET params that define
     * the search parameters.
     *
     * @return "redirect:/raop/searchcrr/search
     */
    @RequestMapping(method = RequestMethod.POST)
    public String submitSearch(
            @Valid SearchCrrFormBean searchCrrFormBean, BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {

        if (result.hasErrors()) {
            return "raop/searchcrr";
        }
        // When we submit a new search via post, re-set the start row to zero. 
        searchCrrFormBean.setStartRow(0);

        // Store a success message for rendering on the next request after redirect
        // I have had issues with using flash attributes - if the given 
        // searchCertFormBean contains spaces in some of its values, then 
        // the flash attribute does not always render after redirecting to the view
        //redirectAttrs.addFlashAttribute("message", "Search Submitted OK");

        // Now submit to our GET '/search' handler method to run the query. 
        // Note, I do not manually append request attributes as commented out 
        // in the return redirect: because Spring Views can? be cached based on 
        // the view-name, so if you start appending the parameters to the name 
        // like the code below, each view instance will be cached, see:  
        // http://forum.springsource.org/showthread.php?86633-Spring-3-annotated-controllers-redirection-strings-and-appending-parameters-to-URL


        // Copy our post model attributes to redirect attributes in URL 
        redirectAttrs.addAllAttributes(this.getRedirectParams(searchCrrFormBean));
        // Now submit to our GET /search handler to run the query 
        return "redirect:/raop/searchcrr/search";//+"?"+this.buildGetRequestParams(searchCrrFormBean); 
    }


    /**
     * Handle GETs to "/raop/searchcrr/search?search=params&for=dbquery"
     * to perform a DB search.
     * <p>
     * Processes the known GET params and submits a query to the DB. Unknown
     * GET request parameters are ignored. Binding/validation of GET params
     * is done via Spring @ModelAttribute binding with the given SearchCrrFormBean.
     * If only a partial query is specified, e.g. "/search?name=some cn",
     * default values as defined in SearchCrrFormBean are applied. After successful
     * binding/validation/query, the SEARCH_CRR_FORM_BEAN_SESSIONSCOPE model attribute
     * is updated.
     *
     * @return "raop/searchcrr"
     */
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String submitSearchViaGet(
            @Valid @ModelAttribute(SEARCH_CRR_FORM_BEAN_GET_REQUESTSCOPE)
                    SearchCrrFormBean searchCrrFormBeanGetReq, BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {

        //@RequestMapping(method=RequestMethod.GET, params={"gosearch"}) 
        if (result.hasErrors()) {
            log.warn("binding and validation errors on submitViaGet");
            return "raop/searchcrr";
        }
        // Update our session-scoped @ModelAttribute form bean (SEARCH_CRR_FORM_BEAN_SESSIONSCOPE) 
        // with the request-scoped @ModelAttribute get-request form bean 
        // (SEARCH_CRR_FORM_BEAN_GET_REQUESTSCOPE),  then run the DB search. 
        model.addAttribute(SEARCH_CRR_FORM_BEAN_SESSIONSCOPE, searchCrrFormBeanGetReq);
        this.runSearchUpdateResultsInSession(searchCrrFormBeanGetReq, session);
        model.addAttribute("searchOk", "Search Submitted/Refreshed OK");
        return "raop/searchcrr";
    }


    private void runSearchUpdateResultsInSession(SearchCrrFormBean searchCrrFormBean, HttpSession session) {
        session.setAttribute("lastCRRSearchDate_session", new Date());
        int limit = searchCrrFormBean.getShowRowCount();
        int startRow = searchCrrFormBean.getStartRow();
        Pair<List<CrrRow>, Integer> pair = this.submitCrrSearch(searchCrrFormBean, limit, startRow);
        List<CrrRow> rows = pair.first;
        int totalRows = pair.second;

        // Typically, a PagedListHolder instance will be instantiated with a list of beans, 
        // put into the session, and exported as model (in populateModel as we redirect).
        PartialPagedListHolder<CrrRow> pagedListHolder = new PartialPagedListHolder<CrrRow>(rows, totalRows);
        pagedListHolder.setPageSize(limit);
        pagedListHolder.setRow(startRow);
        session.setAttribute(CRR_PAGE_LIST_HOLDER_SESSIONSCOPE, pagedListHolder);
    }

    private Map<String, Object> getRedirectParams(SearchCrrFormBean searchCrrFormBean) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        if (StringUtils.hasText(searchCrrFormBean.getName())) {
            params.put("name", searchCrrFormBean.getName());
        }
        if (StringUtils.hasText(searchCrrFormBean.getDn())) {
            params.put("dn", searchCrrFormBean.getDn());
        }
        if (StringUtils.hasText(searchCrrFormBean.getData())) {
            params.put("data", searchCrrFormBean.getData());
        }
        if (StringUtils.hasText(searchCrrFormBean.getRa())) {
            params.put("ra", searchCrrFormBean.getRa());
        }
        if (StringUtils.hasText(searchCrrFormBean.getStatus())) {
            params.put("status", searchCrrFormBean.getStatus());
        }
        if (searchCrrFormBean.getCrr_key() != null) {
            params.put("crr_key", searchCrrFormBean.getCrr_key().toString());
        }
        if (searchCrrFormBean.getStartRow() != null) {
            params.put("startRow", searchCrrFormBean.getStartRow().toString());
        }
        if (searchCrrFormBean.getShowRowCount() != null) {
            params.put("showRowCount", searchCrrFormBean.getShowRowCount().toString());
        }
        return params;
    }
    
    /*private String buildGetRequestParams(SearchCrrFormBean searchCrrFormBean) {
         try {
            String enc = "UTF-8";
            StringBuilder params = new StringBuilder("");
            if (StringUtils.hasText(searchCrrFormBean.getName())) {
                params.append("name=");
                params.append(URLEncoder.encode(searchCrrFormBean.getName(), enc));
                params.append("&");
            }
            if(StringUtils.hasText(searchCrrFormBean.getDn())){
                params.append("dn=");
                params.append(URLEncoder.encode(searchCrrFormBean.getDn(), enc));
                params.append("&"); 
            }
            if(StringUtils.hasText(searchCrrFormBean.getData())){
                params.append("data=");
                params.append(URLEncoder.encode(searchCrrFormBean.getData(), enc));
                params.append("&");
            }
            if(StringUtils.hasText(searchCrrFormBean.getRa())){
                params.append("ra=");
                params.append(URLEncoder.encode(searchCrrFormBean.getRa(), enc));
                params.append("&");
            }
            if(StringUtils.hasText(searchCrrFormBean.getStatus())){
                params.append("status=");
                params.append(URLEncoder.encode(searchCrrFormBean.getStatus(), enc));
                params.append("&");
            }
            if(searchCrrFormBean.getCrr_key() != null){
                params.append("crr_key=");
                params.append(URLEncoder.encode(searchCrrFormBean.getCrr_key().toString(), enc));
                params.append("&");
            }
            if(searchCrrFormBean.getStartRow() != null){ 
                params.append("startRow=");
                params.append(URLEncoder.encode(searchCrrFormBean.getStartRow().toString(), enc));
                params.append("&");
            }
            if(searchCrrFormBean.getShowRowCount() != null){
                params.append("showRowCount=");
                params.append(URLEncoder.encode(searchCrrFormBean.getShowRowCount().toString(), enc));
                params.append("&");
            }
            
            // remove final & if present 
            String getParams = params.toString();
            if (getParams.endsWith("&")) {
                getParams = getParams.substring(0, getParams.length() - 1);
            }
            return getParams;
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
     }*/


    /**
     * Handle POSTs to "/raop/searchcrr/page" to paginate to a specific row.
     *
     * @return "redirect:/raop/searchcrr/search?search=params&for=dbquery"
     */
    @RequestMapping(value = "/goto", method = RequestMethod.POST)
    public String submitGotoPage(
            @Valid @ModelAttribute("gotoPageFormBean") GotoPageNumberFormBean gotoPageFormBean,
            BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {

        if (result.hasErrors()) {
            return "raop/searchcrr";
        }
        if (session.getAttribute(SEARCH_CRR_FORM_BEAN_SESSIONSCOPE) == null ||
                session.getAttribute(CRR_PAGE_LIST_HOLDER_SESSIONSCOPE) == null) {
            return "raop/searchcrr"; // session probably expired
        }
        // Get the search data from session. 
        SearchCrrFormBean searchCrrFormBean =
                (SearchCrrFormBean) session.getAttribute(SEARCH_CRR_FORM_BEAN_SESSIONSCOPE);
        PartialPagedListHolder<CrrRow> pagedListHolder =
                (PartialPagedListHolder<CrrRow>) session.getAttribute(CRR_PAGE_LIST_HOLDER_SESSIONSCOPE);

        // update requested start row 
        int startRow = gotoPageFormBean.getGotoPageNumber() - 1; //zero offset when used in SQL  
        //int limit = pagedListHolder.getPageSize();

        // If requested start row is > than total rows or < 0, just return        
        if (startRow > pagedListHolder.getTotalRows() || startRow < 0) {
            return "redirect:/raop/searchcrr";
        }
        // Update requested start row and re-submit the search as a GET request  
        searchCrrFormBean.setStartRow(startRow);
        redirectAttrs.addAllAttributes(this.getRedirectParams(searchCrrFormBean));
        return "redirect:/raop/searchcrr/search";//+"?"+this.buildGetRequestParams(searchCrrFormBean);
    }


    /**
     * Handle GETs to "/raop/searchcrr/page" with URL parameters. Used for paging
     * through the crr list when clicking Next,Prev,First,Last links.
     *
     * @return "redirect:/raop/searchcrr/search?multiple=search&params=go here"
     */
    @RequestMapping(value = "page", method = RequestMethod.GET)
    public String handlePageRequest(@RequestParam(required = false) String page,
                                    HttpSession session, RedirectAttributes redirectAttrs) {

        // Get the search data from session. 
        SearchCrrFormBean searchCrrFormBean =
                (SearchCrrFormBean) session.getAttribute(SEARCH_CRR_FORM_BEAN_SESSIONSCOPE);
        PartialPagedListHolder<CrrRow> pagedListHolder =
                (PartialPagedListHolder<CrrRow>) session.getAttribute(CRR_PAGE_LIST_HOLDER_SESSIONSCOPE);

        if ("next".equals(page) || "prev".equals(page) || "first".equals(page) || "last".equals(page)) {
            int limit = searchCrrFormBean.getShowRowCount(); //pagedListHolder.getPageSize();
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
            // Update requested start row and re-submit the search as a GET request  
            searchCrrFormBean.setStartRow(startRow);
            redirectAttrs.addAllAttributes(this.getRedirectParams(searchCrrFormBean));
            return "redirect:/raop/searchcrr/search";//+"?"+this.buildGetRequestParams(searchCrrFormBean);
        }
        return "redirect:/raop/searchcrr";
    }


    /**
     * Query the DB for a list of request rows and for the total number of
     * rows found that match the search criteria defined by SearchCrrFormBean.
     *
     * @param searchCrrFormBean Used to specify search criteria.
     * @param limit             Limits the number of search results.
     * @param offset            Offset the results by this many rows.
     * @return <code>Pair.first</code> holds the list of crr rows (from
     * offset to limit), while <code>Pair.second</code> is the total number of
     * matching rows found in the DB.
     */
    private Pair<List<CrrRow>, Integer> submitCrrSearch(
            SearchCrrFormBean searchCrrFormBean, Integer limit, Integer offset) {

        if (searchCrrFormBean.getCrr_key() != null) {
            CrrRow row = this.jdbcCrrDao.findById(searchCrrFormBean.getCrr_key());
            List<CrrRow> rows = new ArrayList<CrrRow>(0);
            if (row != null) {
                rows.add(row);
                row.setData(null);
            }
            return Pair.create(rows, rows.size());

        } else {
            // select RA from pulldown and perform search 
            Map<JdbcCrrDao.WHERE_PARAMS, String> whereParams = new EnumMap<JdbcCrrDao.WHERE_PARAMS, String>(JdbcCrrDao.WHERE_PARAMS.class);

            String ra = searchCrrFormBean.getRa();
            if (StringUtils.hasText(ra) && !"all".equals(ra)) {
                // TODO - ra like search (note ra column is all null in DB, so specifying a value will not work!) 
                String[] ou_l = ra.split("_");
                if (ou_l != null && ou_l.length >= 1) {
                    // typical ra input: 'CLRC DL'  (converted to 'L=DL,OU=CLRC') 
                    String raSearch;
                    if (ou_l.length == 1) {
                        // assume search string is L only 
                        raSearch = "L=" + ou_l[0];
                    } else {
                        raSearch = "L=" + ou_l[1] + ",OU=" + ou_l[0];
                    }
                    log.debug(" raSearch is: " + raSearch);
                    whereParams.put(JdbcCrrDao.WHERE_PARAMS.DN_HAS_RA_LIKE, this.prepareLikeValue(raSearch));
                }
            }
            String status = searchCrrFormBean.getStatus();
            if (StringUtils.hasText(status) && !"all".equals(status)) {
                whereParams.put(JdbcCrrDao.WHERE_PARAMS.STATUS_EQ, status);
            }
            String name = searchCrrFormBean.getName();
            if (StringUtils.hasText(name)) {
                whereParams.put(JdbcCrrDao.WHERE_PARAMS.CN_LIKE, this.prepareLikeValue(name));
            }
            String dn = searchCrrFormBean.getDn();
            if (StringUtils.hasText(dn)) {
                whereParams.put(JdbcCrrDao.WHERE_PARAMS.DN_LIKE, this.prepareLikeValue(dn));
            }
            String data = searchCrrFormBean.getData();
            if (StringUtils.hasText(data)) {
                whereParams.put(JdbcCrrDao.WHERE_PARAMS.DATA_LIKE, this.prepareLikeValue(data));
            }
            List<CrrRow> rows = this.jdbcCrrDao.findBy(whereParams, limit, offset);
            rows = this.jdbcCrrDao.setSubmitDateFromData(rows);
            int totalRows = jdbcCrrDao.countBy(whereParams);
            // Nullify the Data property before we add the rows to HTTP session to keep
            // the memory requirements low. 
            for (CrrRow row : rows) {
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
    public void setRalistDao(JdbcRalistDao ralistDao) {
        this.ralistDao = ralistDao;
    }

    @Inject
    public void setJdbcRequestDao(JdbcCrrDao jdbcCrrDao) {
        this.jdbcCrrDao = jdbcCrrDao;
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }
}
