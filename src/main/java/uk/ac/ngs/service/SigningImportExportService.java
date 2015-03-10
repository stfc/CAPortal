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
