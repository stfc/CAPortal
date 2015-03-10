/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ngs.service;

import java.io.File;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class to manage import/export of signing data to/from the DB.
 *
 * @author David Meredith
 */
public class SigningImportExportService {

    @Transactional
    public synchronized void importSignedData(File importDir, String importLockFileName) {
        // Check lockFile does not exist, throw exception if true 
        //  Requires manual intervention - lock file exists but only this synchronized 
        //  method creates/deletes the lock so something has gone wrong - probably 
        //  need to manually delete the lock file.  
        // 
        // Check tarball exists, return if it does not (nothing to do) 
        // Create lockFile
        // Decompress tar file 
        // Iterate import dirs/files and issue insert into statements for CSRs/CRRs
        //   compose log on the way of actions 
        // finally { Delete lockfile }
    }

}
