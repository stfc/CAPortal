--select * from certificate where dn like '%ra meredith%'
--select * from raoplist where cn like '%meredith%'
--select * from request where ra = 'CLRC DL' and (status = 'NEW' or status='RENEW') and role = 'User'

select cert_key, data, dn, email, status, role, notafter from certificate
    where status = 'VALID' and dn like '%meredith%' and notafter > '20121205121212'; 