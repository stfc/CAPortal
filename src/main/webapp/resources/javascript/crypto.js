/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 * Create a new CSR using Forge JS with a 2048 key strength.
 *
 * @param pw
 * @param cn
 * @param ou
 * @param loc
 * @param o
 * @param c
 * @param pw
 */
function createCSR(cn, ou, loc, o, c, pw) {

    console.log('Generating 2048-bit key-pair...');
    const keys = forge.pki.rsa.generateKeyPair({bits: 2048, workers: -1});
    console.log('Key-pair created.');

    console.log('Creating certification request (CSR) ...');
    const csr = forge.pki.createCertificationRequest();
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


    // sign certification request
    csr.sign(keys.privateKey);
    console.log('Certification request (CSR) created.');

    // PEM-format keys and csr
    const algOpts = {algorithm: 'aes256', prfAlgorithm: 'sha256'};
    const pem = {
        //privateKey: forge.pki.privateKeyToPem(keys.privateKey),
        privateKey: forge.pki.encryptRsaPrivateKey(keys.privateKey, pw, algOpts),
        publicKey: forge.pki.publicKeyToPem(keys.publicKey),
        csr: forge.pki.certificationRequestToPem(csr)
    };

    return pem;
}
            