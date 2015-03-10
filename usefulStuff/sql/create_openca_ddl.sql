--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'SQL_ASCII';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

--
-- Name: bulk_update(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION bulk_update() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    source VARCHAR; target VARCHAR;
    raop VARCHAR; time VARCHAR;
    cnt RECORD; row RECORD;
BEGIN
    source := TG_ARGV[ 0 ];
    target := TG_ARGV[ 1 ];
    -- source = NEW or RENEW, target = APPROVED


    -- if (bulkId != null && old.status=NEW|RENEW && new.status=APPROVED) 
    IF NEW.bulk IS NOT NULL AND source = OLD.status AND target = NEW.status THEN

        -- Only one (the first) approved request triggers the UPDATE (of this bulk batch).
        -- DM: This is an UPDATE AFTER ROW trigger - it is called after an UPDATE of individual request ROWs one     
        -- at a time. Therefore, since we know that this rows old.status was either NEW or RENEW   
        -- and its new.status is APPROVED, then cnt will be one AFTER  update of the FIRST row
        -- with this bulk id.    
        -- 
        FOR cnt IN SELECT COUNT(*) AS count FROM request WHERE bulk = NEW.bulk AND status = NEW.status LOOP
            IF cnt.count != 1 THEN
                RETURN NEW;
            END IF;
        END LOOP;


        -- Get RAOP and LAST_ACTION_DATE values from the data col of the newly updated row that triggered the function.
        -- DM: Note, for substring func, if the pattern contains any parentheses, the portion of the 
        -- text that matched the first parenthesized subexpression 
        -- (the one whose left parenthesis comes first) is returned. See: 
        -- http://www.postgresql.org/docs/8.1/static/functions-matching.html 
        -- DM: do we assume these key=value pairs will have been added and evaluated during the approval process - YES we do, they are added by approval. 
        --   Also, the whitespace pattern before and after = char [ \t]? should prob be [ \t]*
        -- 
        raop := SUBSTRING( NEW.data FROM 'RAOP[ \t]?=[ \t]?([0-9]+)' );
        time := SUBSTRING( NEW.data FROM 'LAST_ACTION_DATE[ \t]?=[ \t]?([^\n]+)' );

        -- Find all the records that need to be UPDATEd.
        -- DM: Appropriate records will have same bulk id and the same OLD.status  
        -- (e.g. NEW|RENEW) as the (first) record that triggered the function. 
        -- Note, because this is an UPDATE AFTER ROW trigger, using status = OLD.status will 
        -- (intentionally) exclude the already updated row that triggered the function in the loop below! 
        --  
        FOR row IN SELECT * FROM request WHERE bulk = NEW.bulk AND status = OLD.status LOOP

            -- Insert LAST_ACTION_DATE and RAOP keys if the fields dont exist.
            -- DM: The RAOP field is only searched for, what about an existing LAST_ACTION_DATE?
            -- (LAST_ACTION_DATE will not normally be present - however, lets cater for it 
            -- being present). 
            -- 
            IF SUBSTRING( row.data FROM 'RAOP[ \t]?=' ) IS NULL THEN
                row.data := REPLACE( row.data, '-----END HEADER-----', 'LAST_ACTION_DATE =
RAOP =
-----END HEADER-----' );
            END IF;

            -- Update LAST_ACTION_DATE.
            -- Note, no parentheses used in substring to return part of the pattern (See above) 
            row.data := REPLACE( row.data,
                                 SUBSTRING( row.data FROM 'LAST_ACTION_DATE[ \t]?=[ \t]?[^\n]*' ),
                                 'LAST_ACTION_DATE = ' || time );

            -- Update RAOP.
            -- Note, no parentheses used to return part of the pattern (See above) 
            row.data := REPLACE( row.data,
                                 SUBSTRING( row.data FROM 'RAOP[ \t]?=[ \t]?[^\n]*' ),
                                 'RAOP = ' || raop );         

            EXECUTE 'UPDATE request'
                    || ' SET status = ' || quote_literal( NEW.status ) || ', data = ' || quote_literal( row.data )
                    || ' WHERE req_key = ' || row.req_key;

        END LOOP;

    END IF;

    RETURN NEW;
END;

$$;


--
-- Name: plpgsql_call_handler(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION plpgsql_call_handler() RETURNS language_handler
    LANGUAGE c
    AS '$libdir/plpgsql', 'plpgsql_call_handler';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: bulk_chain; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE bulk_chain (
    oldid integer NOT NULL,
    newid integer NOT NULL
);


--
-- Name: ca_certificate; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE ca_certificate (
    ca_cert_key text NOT NULL,
    format text,
    data text,
    dn text,
    cn text,
    email text,
    status text,
    public_key text,
    notafter bigint
);


--
-- Name: certificate; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE certificate (
    cert_key bigint NOT NULL,
    format text,
    data text,
    dn text,
    cn text,
    email text,
    status text,
    role text,
    public_key text,
    notafter bigint,
    req_key bigint,
    loa text
);


--
-- Name: crl; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE crl (
    crl_key text NOT NULL,
    status text,
    format text,
    data text,
    last_update text,
    next_update text
);


--
-- Name: crr; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE crr (
    crr_key bigint NOT NULL,
    cert_key bigint,
    submit_date text,
    format text,
    data text,
    dn text,
    cn text,
    email text,
    ra text,
    rao text,
    status text,
    reason text,
    loa text
);


--
-- Name: request; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE request (
    req_key bigint NOT NULL,
    format text,
    data text,
    dn text,
    cn text,
    email text,
    ra text,
    rao text,
    status text,
    role text,
    public_key text,
    scep_tid text,
    loa text,
    bulk integer,
    exported boolean DEFAULT false
);


--
-- Name: exportable_request; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW exportable_request AS
 SELECT request.req_key,
    request.format,
    request.data,
    request.dn,
    request.cn,
    request.email,
    request.ra,
    request.rao,
    request.status,
    request.role,
    request.public_key,
    request.scep_tid,
    request.loa,
    request.bulk,
    request.exported
   FROM request
  WHERE ((request.exported IS NULL) OR (request.exported IS FALSE));


--
-- Name: pppk; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE pppk (
    pppk_key bigint NOT NULL,
    nonce text,
    ntimeout bigint,
    keyid text,
    opaque text,
    otimeout bigint,
    realm text
);


--
-- Name: ralist; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE ralist (
    ra_id integer NOT NULL,
    order_id integer NOT NULL,
    ou text NOT NULL,
    l text NOT NULL,
    active boolean
);


--
-- Name: raoplist; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE raoplist (
    ou text NOT NULL,
    l text NOT NULL,
    name text NOT NULL,
    email text NOT NULL,
    phone text NOT NULL,
    street text,
    city text,
    postcode text,
    cn text,
    manager boolean,
    operator boolean,
    trainingdate date,
    title text,
    conemail text,
    location text,
    ra_id integer,
    department_hp text,
    institute_hp text,
    active boolean,
    ra_id2 integer
);


--
-- Name: seq_bulk; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_bulk
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: seq_pppk; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_pppk
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bulk_chain_newid_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY bulk_chain
    ADD CONSTRAINT bulk_chain_newid_key UNIQUE (newid);


--
-- Name: bulk_chain_oldid_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY bulk_chain
    ADD CONSTRAINT bulk_chain_oldid_key UNIQUE (oldid);


--
-- Name: ca_certificate_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY ca_certificate
    ADD CONSTRAINT ca_certificate_pkey PRIMARY KEY (ca_cert_key);


--
-- Name: certificate_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY certificate
    ADD CONSTRAINT certificate_pkey PRIMARY KEY (cert_key);


--
-- Name: crl_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY crl
    ADD CONSTRAINT crl_pkey PRIMARY KEY (crl_key);


--
-- Name: crr_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY crr
    ADD CONSTRAINT crr_pkey PRIMARY KEY (crr_key);


--
-- Name: pppk_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY pppk
    ADD CONSTRAINT pppk_pkey PRIMARY KEY (pppk_key);


--
-- Name: ralist_order_id_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY ralist
    ADD CONSTRAINT ralist_order_id_key UNIQUE (order_id);


--
-- Name: ralist_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY ralist
    ADD CONSTRAINT ralist_pkey PRIMARY KEY (ra_id);


--
-- Name: request_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY request
    ADD CONSTRAINT request_pkey PRIMARY KEY (req_key);


--
-- Name: $1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY raoplist
    ADD CONSTRAINT "$1" FOREIGN KEY (ra_id2) REFERENCES ralist(ra_id);


--
-- Name: public; Type: ACL; Schema: -; Owner: -
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- Name: ca_certificate; Type: ACL; Schema: public; Owner: -
--

REVOKE ALL ON TABLE ca_certificate FROM PUBLIC;
REVOKE ALL ON TABLE ca_certificate FROM postgres;
GRANT ALL ON TABLE ca_certificate TO postgres;


--
-- Name: certificate; Type: ACL; Schema: public; Owner: -
--

REVOKE ALL ON TABLE certificate FROM PUBLIC;
REVOKE ALL ON TABLE certificate FROM postgres;
GRANT ALL ON TABLE certificate TO postgres;


--
-- Name: crl; Type: ACL; Schema: public; Owner: -
--

REVOKE ALL ON TABLE crl FROM PUBLIC;
REVOKE ALL ON TABLE crl FROM postgres;
GRANT ALL ON TABLE crl TO postgres;


--
-- Name: crr; Type: ACL; Schema: public; Owner: -
--

REVOKE ALL ON TABLE crr FROM PUBLIC;
REVOKE ALL ON TABLE crr FROM postgres;
GRANT ALL ON TABLE crr TO postgres;


--
-- Name: request; Type: ACL; Schema: public; Owner: -
--

REVOKE ALL ON TABLE request FROM PUBLIC;
REVOKE ALL ON TABLE request FROM postgres;
GRANT ALL ON TABLE request TO postgres;


--
-- Name: pppk; Type: ACL; Schema: public; Owner: -
--

REVOKE ALL ON TABLE pppk FROM PUBLIC;
REVOKE ALL ON TABLE pppk FROM postgres;
GRANT ALL ON TABLE pppk TO postgres;


--
-- Name: ralist; Type: ACL; Schema: public; Owner: -
--

REVOKE ALL ON TABLE ralist FROM PUBLIC;
REVOKE ALL ON TABLE ralist FROM postgres;
GRANT ALL ON TABLE ralist TO postgres;


--
-- Name: raoplist; Type: ACL; Schema: public; Owner: -
--

REVOKE ALL ON TABLE raoplist FROM PUBLIC;
REVOKE ALL ON TABLE raoplist FROM postgres;
GRANT ALL ON TABLE raoplist TO postgres;


--
-- Name: seq_pppk; Type: ACL; Schema: public; Owner: -
--

REVOKE ALL ON SEQUENCE seq_pppk FROM PUBLIC;
REVOKE ALL ON SEQUENCE seq_pppk FROM postgres;
GRANT ALL ON SEQUENCE seq_pppk TO postgres;


--
-- PostgreSQL database dump complete
--

