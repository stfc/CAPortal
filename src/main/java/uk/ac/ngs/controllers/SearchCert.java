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
import uk.ac.ngs.dao.JdbcCertificateDao;
import uk.ac.ngs.dao.JdbcRalistDao;
import uk.ac.ngs.domain.CertificateRow;
import uk.ac.ngs.domain.RalistRow;
import uk.ac.ngs.forms.GotoPageNumberFormBean;
import uk.ac.ngs.forms.SearchCertFormBean;
import uk.ac.ngs.security.SecurityContextService;
import uk.ac.ngs.service.CertUtil;

import javax.inject.Inject;
import jakarta.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.*;

/**
 * Controller for the "/raop/searchcert" page certificate searches.
 * <p>
 * The controller handles pagination through certificate domain object results.
 * Searches can be submitted via GET or POST - both do the same validation.
 * On submission of a form POST, posted params are validated and the request
 * is re-directed to the GET handler with the appropriate GET request params
 * for the search. This way, a similar code path is used to execute DB searches
 * (same apart from the GET/POST entry point).
 * <p>
 *
 * @author David Meredith
 */
@Controller
@RequestMapping("/raop/searchcert")
@SessionAttributes(value = {SearchCert.SEARCH_CERT_FORM_BEAN_SESSIONSCOPE})
@Secured("ROLE_RAOP")
public class SearchCert {

    private static final Log log = LogFactory.getLog(SearchCert.class);
    private SecurityContextService securityContextService;
    private JdbcCertificateDao certDao;
    private JdbcRalistDao ralistDao;
    /**
     * Name of the model attribute used to bind form POSTs.
     */
    public static final String SEARCH_CERT_FORM_BEAN_SESSIONSCOPE = "searchCertFormBean";
    /**
     * Name of the model attribute used to bind GET request search parameters.
     */
    public static final String SEARCH_CERT_FORM_BEAN_GET_REQUESTSCOPE = "searchCertFormBean_REQUESTSCOPE";
    /**
     * Name of model attribute that stores search results in session. Can be
     * accessed in JSP using <code>${sessionScope.certSearchPageHolder}</code>
     */
    public static final String CERT_PAGE_LIST_HOLDER_SESSIONSCOPE = "certSearchPageHolder";

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
        //log.debug("populateModel");
        // Populate model with an empty list if session var is not present (e.g. session expired) 
        PartialPagedListHolder<CertificateRow> pagedListHolder;
        Object testNotNull = session.getAttribute(SearchCert.CERT_PAGE_LIST_HOLDER_SESSIONSCOPE);
        if (testNotNull == null) {
            pagedListHolder = new PartialPagedListHolder<>(new ArrayList<>(0));
            session.setAttribute(SearchCert.CERT_PAGE_LIST_HOLDER_SESSIONSCOPE, pagedListHolder);
        } else {
            pagedListHolder = (PartialPagedListHolder<CertificateRow>) testNotNull;
        }

        // Populate the RA list pull down
        List<RalistRow> rows = this.ralistDao.findAllByActive(true, null, null);
        List<String> raArray = new ArrayList<>(rows.size());
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
        model.addAttribute(RA_ARRAY_REQUESTSCOPE, raArray.toArray());
    }

    /**
     * Invoked initially to add the 'searchCertFormBean' model attribute. Once
     * created the SearchCert.SEARCH_CERT_FORM_BEAN comes from the HTTP session (see
     * SessionAttributes annotation)
     *
     * @return
     */
    @ModelAttribute(SearchCert.SEARCH_CERT_FORM_BEAN_SESSIONSCOPE)
    public SearchCertFormBean createFormBean() {
        return new SearchCertFormBean();
    }

    @ModelAttribute(SEARCH_CERT_FORM_BEAN_GET_REQUESTSCOPE)
    public SearchCertFormBean createGetRequestFormBean() {
        return new SearchCertFormBean();
    }

    /**
     * Add the 'gotoPageFormBean' to the model if not present.
     *
     * @return
     */
    @ModelAttribute("gotoPageFormBean")
    public GotoPageNumberFormBean createGotoFormBean() {
        return new GotoPageNumberFormBean();
    }


    /**
     * Handle GETs to <pre>/raop/searchcert</pre> for Idempotent page refreshes.
     */
    @RequestMapping(method = RequestMethod.GET)
    public String handleGetRequest() {
        return "raop/searchcert";
    }

    //@InitBinder
    // protected void initBinder(WebDataBinder binder) {
    // binder.setValidator(new FooValidator()); }
    //@RequestMapping(value = "/search", method = RequestMethod.POST)


    /**
     * Handle POSTs to "/raop/searchcert" to do a certificate search.
     * The method processes the form input and then redirects to the "/search"
     * URL (handled in this controller) with URL encoded GET params that define
     * the search parameters.
     *
     * @return "redirect:/raop/searchcert/search
     */
    @RequestMapping(method = RequestMethod.POST)
    public String submitSearchViaPost(
            @Valid @ModelAttribute(SEARCH_CERT_FORM_BEAN_SESSIONSCOPE) SearchCertFormBean searchCertFormBean, BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {

        if (result.hasErrors()) {
            log.warn("binding and validation errors");
            return "raop/searchcert";
        }
        // When we submit a new search via post, re-set the start row to zero. 
        searchCertFormBean.setStartRow(0);

        // Store a success message for rendering on the next request after redirect
        // I have had issues with using flash attributes - if the given 
        // searchCertFormBean contains spaces in some of its values, then 
        // the flash attribute does not always render after redirecting to the view
        //redirectAttrs.addFlashAttribute("message", "Search Submitted OK");

        // Copy our post model attributes to redirect attributes in URL 
        redirectAttrs.addAllAttributes(this.getRedirectParams(searchCertFormBean));
        // Now submit to our GET '/search' handler method to run the query. 
        // Note, I do not manually append request attributes as commented out 
        // in the return redirect: because Spring Views can? be cached based on 
        // the view-name, so if you start appending the parameters to the name 
        // like the code below, each view instance will be cached, see:  
        // http://forum.springsource.org/showthread.php?86633-Spring-3-annotated-controllers-redirection-strings-and-appending-parameters-to-URL
        return "redirect:/raop/searchcert/search";//+"?"+this.buildGetRequestParams(searchCertFormBean); 
    }

    /**
     * Handle GETs to "/raop/searchcert/search?search=params&for=dbquery"
     * to perform a DB search.
     * <p>
     * Processes the known GET params and submits a query to the DB. Unknown
     * GET request parameters are ignored. Binding/validation of GET params
     * is done via Spring @ModelAttribute binding with the given SearchCertFormBean.
     * If only a partial query is specified, e.g. "/search?name=some cn",
     * default values as defined in SearchCertFormBean are applied. After successful
     * binding/validation/query, the SEARCH_CERT_FORM_BEAN_SESSIONSCOPE model attribute
     * is updated.
     *
     * @return "raop/searchcert"
     */
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String submitSearchViaGet(
            @Valid @ModelAttribute(SEARCH_CERT_FORM_BEAN_GET_REQUESTSCOPE)
                    SearchCertFormBean searchCertFormBeanGetReq, BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {

        //@RequestMapping(method=RequestMethod.GET, params={"gosearch"}) 
        if (result.hasErrors()) {
            log.warn("binding and validation errors on submitViaGet");
            return "raop/searchcert";
        }
        // Update our session-scoped @ModelAttribute form bean (SEARCH_CERT_FORM_BEAN_SESSIONSCOPE) 
        // with the request-scoped @ModelAttribute get-request form bean 
        // (SEARCH_CERT_FORM_BEAN_GET_REQUESTSCOPE),  then run the DB search. 
        model.addAttribute(SEARCH_CERT_FORM_BEAN_SESSIONSCOPE, searchCertFormBeanGetReq);
        this.runSearchUpdateResultsInSession(searchCertFormBeanGetReq, session);
        model.addAttribute("searchOk", "Search Submitted/Refreshed OK");
        return "raop/searchcert";
    }

    /**
     * Use the given SearchCertFormBean to submit the DB search and put the results
     * in session under the CERT_PAGE_LIST_HOLDER session attribute.
     */
    private void runSearchUpdateResultsInSession(SearchCertFormBean searchCertFormBean, HttpSession session) {
        // reset the last search date and limit/offset vals. 
        session.setAttribute("lastCertSearchDate_session", new Date());
        int limit = searchCertFormBean.getShowRowCount();
        int startRow = searchCertFormBean.getStartRow();
        Pair<List<CertificateRow>, Integer> results =
                this.submitCertificateSearch(searchCertFormBean, limit, startRow);
        List<CertificateRow> rows = results.first;
        Integer totalRows = results.second;
        session.setAttribute("searchCertTotalRows_session", totalRows);

        // Typically, a PagedListHolder instance will be instantiated with a list of beans, 
        // put into the session, and exported as model (in populateModel as we redirect).
        PartialPagedListHolder<CertificateRow> pagedListHolder = new PartialPagedListHolder<>(rows, totalRows);
        pagedListHolder.setPageSize(limit);
        pagedListHolder.setRow(startRow);
        session.setAttribute(SearchCert.CERT_PAGE_LIST_HOLDER_SESSIONSCOPE, pagedListHolder);
        // If no validation errors, typically you would e.g. save to a db and clear the 
        // "session form bean" attribute from the session via SessionStatus.setComplete().
        //sessionStatus.setComplete(); 
    }

    private Map<String, Object> getRedirectParams(SearchCertFormBean searchCertFormBean) {
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.hasText(searchCertFormBean.getName())) {
            params.put("name", searchCertFormBean.getName());
        }
        if (StringUtils.hasText(searchCertFormBean.getDn())) {
            params.put("dn", searchCertFormBean.getDn());
        }
        if (StringUtils.hasText(searchCertFormBean.getData())) {
            params.put("data", searchCertFormBean.getData());
        }
        if (StringUtils.hasText(searchCertFormBean.getEmailAddress())) {
            params.put("emailAddress", searchCertFormBean.getEmailAddress());
        }
        if (searchCertFormBean.getSearchNullEmailAddress() != null) {
            params.put("searchNullEmailAddress", searchCertFormBean.getSearchNullEmailAddress().toString());
        }
        if (StringUtils.hasText(searchCertFormBean.getRole())) {
            params.put("role", searchCertFormBean.getRole());
        }
        if (StringUtils.hasText(searchCertFormBean.getStatus())) {
            params.put("status", searchCertFormBean.getStatus());
        }
        if (searchCertFormBean.getNotExpired() != null) {
            params.put("notExpired", searchCertFormBean.getNotExpired().toString());
        }
        if (searchCertFormBean.getStartRow() != null) {
            params.put("startRow", searchCertFormBean.getStartRow().toString());
        }
        if (searchCertFormBean.getSerial() != null) {
            params.put("serial", searchCertFormBean.getSerial().toString());
        }
        if (searchCertFormBean.getShowRowCount() != null) {
            params.put("showRowCount", searchCertFormBean.getShowRowCount().toString());
        }
        if (searchCertFormBean.getRa() != null) {
            params.put("ra", searchCertFormBean.getRa());
        }
        return params;
    }

    /**
     * Handle POSTs to "/raop/searchcert/goto" to paginate to a specific row
     * in the results list.
     *
     * @return "redirect:/raop/searchcert/search?search=params&for=dbquery"
     */
    @RequestMapping(value = "/goto", method = RequestMethod.POST)
    public String submitGotoPage(
            @Valid @ModelAttribute("gotoPageFormBean") GotoPageNumberFormBean gotoPageFormBean,
            BindingResult result,
            RedirectAttributes redirectAttrs, SessionStatus sessionStatus,
            Model model, HttpSession session) {

        if (result.hasErrors()) {
            return "raop/searchcert";
        }
        // Get the search data from session. 
        SearchCertFormBean sessionSearchCertFormBean =
                (SearchCertFormBean) session.getAttribute(SearchCert.SEARCH_CERT_FORM_BEAN_SESSIONSCOPE);
        PartialPagedListHolder<CertificateRow> pagedListHolder =
                (PartialPagedListHolder<CertificateRow>) session.getAttribute(SearchCert.CERT_PAGE_LIST_HOLDER_SESSIONSCOPE);

        int startRow = gotoPageFormBean.getGotoPageNumber() - 1; //zero offset when used in SQL  

        // If requested start row is > than total rows or < 0, just return        
        if (startRow > pagedListHolder.getTotalRows() || startRow < 0) {
            return "raop/searchcert";
        }

        // Update requested start row and re-submit the search as a GET request  
        sessionSearchCertFormBean.setStartRow(startRow);
        redirectAttrs.addAllAttributes(this.getRedirectParams(sessionSearchCertFormBean));
        return "redirect:/raop/searchcert/search"; //+"?"+this.buildGetRequestParams(sessionSearchCertFormBean);  
    }

    /**
     * Handle GETs to '/raop/searchcert/page?page=next|prev|first|last'.
     * Used for paging through the certificate list when clicking Next,Prev,First,Last links.
     *
     * @return "redirect:/raop/searchcert/search?multiple=search&params=go here"
     */
    @RequestMapping(value = "page", method = RequestMethod.GET)
    public String handlePageRequest(@RequestParam(required = false) String page,
                                    HttpSession session, RedirectAttributes redirectAttrs) {
        //String page = request.getParameter("page");
        //http://balusc.blogspot.co.uk/2008/10/effective-datatable-paging-and-sorting.html

        // Get the search data from session. 
        SearchCertFormBean searchCertFormBean =
                (SearchCertFormBean) session.getAttribute(SearchCert.SEARCH_CERT_FORM_BEAN_SESSIONSCOPE);
        PartialPagedListHolder<CertificateRow> pagedListHolder =
                (PartialPagedListHolder<CertificateRow>) session.getAttribute(SearchCert.CERT_PAGE_LIST_HOLDER_SESSIONSCOPE);

        if ("next".equals(page) || "prev".equals(page) || "first".equals(page) || "last".equals(page)) {
            int limit = searchCertFormBean.getShowRowCount(); //pagedListHolder.getPageSize();
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
            searchCertFormBean.setStartRow(startRow);
            redirectAttrs.addAllAttributes(this.getRedirectParams(searchCertFormBean));
            return "redirect:/raop/searchcert/search";//+"?"+this.buildGetRequestParams(searchCertFormBean);  
        }
        return "/raop/searchcert";
    }

    /**
     * Query the DB for a list of certificate rows and for the total number of
     * rows found that match the search criteria defined by SearchCertFormBean.
     *
     * @param searchCertFormBean Used to specify search criteria.
     * @param limit              Limits the number of search results.
     * @param offset             Offset the results by this many rows.
     * @return <code>Pair.first</code> holds the list of certificate rows (from
     * offset to limit), while <code>Pair.second</code> is the total number of
     * matching rows found in the DB.
     */
    private Pair<List<CertificateRow>, Integer> submitCertificateSearch(
            SearchCertFormBean searchCertFormBean, int limit, int offset) {
        //log.debug("Search by: " + this.securityContextService.getUserName());

        // serial number overrides any other search criteria. 
        if (searchCertFormBean.getSerial() != null) {
            CertificateRow row = this.certDao.findById(searchCertFormBean.getSerial());
            List<CertificateRow> rows = new ArrayList<>(0);
            if (row != null) {
                rows.add(row);
                row.setData(null);
            }
            return Pair.create(rows, rows.size());
        } else {
            Map<JdbcCertificateDao.WHERE_PARAMS, String> params = this.getSearchParams(searchCertFormBean);
            List<CertificateRow> rows = this.certDao.findBy(params, limit, offset);
            int totalRows = this.certDao.countBy(params);
            // Nullify the Data property before we add the rows to HTTP session to keep
            // the memory requirements low. 
            for (CertificateRow row : rows) {
                row.setData(null);
            }
            return Pair.create(rows, totalRows);
        }
    }

    /**
     * From the search criteria specified in the given bean, add relevant search
     * criteria to the returned map.
     */
    private Map<JdbcCertificateDao.WHERE_PARAMS, String> getSearchParams(SearchCertFormBean searchCertFormBean) {

        Map<JdbcCertificateDao.WHERE_PARAMS, String> params =
                new EnumMap<>(JdbcCertificateDao.WHERE_PARAMS.class);

        if (StringUtils.hasText(searchCertFormBean.getName())) {
            params.put(JdbcCertificateDao.WHERE_PARAMS.CN_LIKE, this.prepareLikeValue(searchCertFormBean.getName()));
        }
        if (StringUtils.hasText(searchCertFormBean.getDn())) {
            params.put(JdbcCertificateDao.WHERE_PARAMS.DN_LIKE, this.prepareLikeValue(searchCertFormBean.getDn()));
        }
        // if search null email address checkbox is true, then we want to search 
        // for email values that are null
        if (searchCertFormBean.getSearchNullEmailAddress()) {
            params.put(JdbcCertificateDao.WHERE_PARAMS.EMAIL_EQ, null);
        } else {
            if (StringUtils.hasText(searchCertFormBean.getEmailAddress())) {
                params.put(JdbcCertificateDao.WHERE_PARAMS.EMAIL_LIKE, this.prepareLikeValue(searchCertFormBean.getEmailAddress()));
            }
        }
        if (StringUtils.hasText(searchCertFormBean.getRole()) && !"all".equals(searchCertFormBean.getRole())) {
            params.put(JdbcCertificateDao.WHERE_PARAMS.ROLE_LIKE, this.prepareLikeValue(searchCertFormBean.getRole()));
        }
        if (StringUtils.hasText(searchCertFormBean.getStatus()) && !"all".equals(searchCertFormBean.getStatus())) {
            params.put(JdbcCertificateDao.WHERE_PARAMS.STATUS_LIKE, this.prepareLikeValue(searchCertFormBean.getStatus()));
        }
        if (StringUtils.hasText(searchCertFormBean.getData())) {
            params.put(JdbcCertificateDao.WHERE_PARAMS.DATA_LIKE, this.prepareLikeValue(searchCertFormBean.getData()));
        }
        if (searchCertFormBean.getNotExpired()) {
            params.put(JdbcCertificateDao.WHERE_PARAMS.NOTAFTER_GREATERTHAN_CURRENTTIME, null);
        }
        String ra = searchCertFormBean.getRa();
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
                params.put(JdbcCertificateDao.WHERE_PARAMS.DN_HAS_RA_LIKE, this.prepareLikeValue(raSearch));
            }
        }
        return params;
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
    public void setJdbcCertificateDao(JdbcCertificateDao dao) {
        this.certDao = dao;
    }

    @Inject
    public void setSecurityContextService(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    @Inject
    public void setRalistDao(JdbcRalistDao ralistDao) {
        this.ralistDao = ralistDao;
    }
}
