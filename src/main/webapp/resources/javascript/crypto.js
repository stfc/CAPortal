/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 * Create a new CSR using Forge JS with a 2048 key strength.
 *
 * @param string cn
 * @param string ou
 * @param string loc
 * @param string o
 * @param string c
 * @param string pw
 * @returns {{privateKey: *, csr: *, publicKey: *}}
 */
function createCSR(cn, ou, loc, o, c, pw) {
    //var forge = require('../js/forge');
    //console.log('Generating 2048-bit key-pair...');
    var keys = forge.pki.rsa.generateKeyPair({bits: 2048, workers: -1});
    console.log('Key-pair created.');

    console.log('Creating certification request (CSR) ...');
    var csr = forge.pki.createCertificationRequest();
    csr.publicKey = keys.publicKey;
    // Notice we have to create the CSR with a DN that has the
    // following RDN structure order (C, O, OU, L, CN)
    // (i.e. starting with C ending with CN)
    csr.setSubject([
        {
            name: 'countryName',
            value: c
        },
        {
            name: 'organizationName',
            value: o
        },
        {
            shortName: 'OU',
            value: ou
        },
        {
            name: 'localityName',
            value: loc
        },
        {
            name: 'commonName',
            value: cn
        }
    ]);

    // add optional attributes
    /*csr.setAttributes([
     {
     name: 'challengePassword',
     value: 'password'
     }, {
     name: 'unstructuredName',
     value: 'My company'
     }
     ]);*/

    // sign certification request
    csr.sign(keys.privateKey);
    console.log('Certification request (CSR) created.');

    // PEM-format keys and csr
    // Specify a decent/recent encryption algorithm. If not specified, the
    // default algorithm is PBEWithMD5AndDES which BouncyCastle/java don't seem
    // to support, hence use 3des.
    var algOpts = {algorithm: '3des'};
    var pem = {
        //privateKey: forge.pki.privateKeyToPem(keys.privateKey),
        privateKey: forge.pki.encryptRsaPrivateKey(keys.privateKey, pw, algOpts),
        publicKey: forge.pki.publicKeyToPem(keys.publicKey),
        csr: forge.pki.certificationRequestToPem(csr)
    };

    //console.log('\nKey-Pair:');
    //console.log(pem.privateKey);
    //console.log(pem.publicKey);
    //console.log('\nCertification Request (CSR):');
    //console.log(pem.csr);

    // verify certification request
    /*try {
     if(csr.verify()) {
     console.log('Certification request (CSR) verified.');
     }
     else {
     throw new Error('Signature not verified.');
     }
     }
     catch(err) {
     console.log('Certification request (CSR) verification failure: ' +
     JSON.stringify(err, null, 2));
     }*/
    return pem;
}
            