/*
Selects the email value of 'OWNEREMAIL = someEmailAddress' from the 'data' column 
of the 'certificate' table where the 'email' column is null (and from some dae until somedate). 
Dates are in: yyyyMMddHHmmss
*/

select substring(data, 'OWNEREMAIL[ \\t]?=[ \\t]?([^\\n]+)') from certificate where 
cn like '%.%'
and (email is null or email = '')
and status = 'VALID'
and (notafter > 20130219093030)
and (notafter < 20130331090101)


/*

// Select cert_key, original request email and email from 'certificate.data' for comparision. 

select certificate.cert_key, request.email as request_email, 
certificate.notafter as cert_not_after, 
substring(certificate.data, 'OWNEREMAIL[ \\t]?=[ \\t]?([^\\n]+)') as cert_data_email

from certificate 
inner join request on certificate.req_key = request.req_key

where 
certificate.cn like '%.%'
and (certificate.email is null or certificate.email = '')
and certificate.status = 'VALID'
-- date format is yyyyMMddHHmmss
and (certificate.notafter > 20130101093030)
--and (certificate.notafter > 20130219093030)
--and (certificate.notafter < 20130331090101)

and (request.status = 'ARCHIVED' or request.status = 'NEW' or request.status = 'APPROVED')*/