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
package uk.ac.ngs.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Read mutable properties from a prop file.
 * Properties can be changed at runtime and the new/updated values will be returned.
 * Supported properties include:
 * <ul>
 *   <li>'hostname.service.values' = A comma separated list of valid service values</li>
 * </ul>
 *
 * @author David Meredith
 */
public class MutableConfigParams {

    private final String propertyFileFullPath;

    public MutableConfigParams(final String propertyFileFullPath) {
        this.propertyFileFullPath = propertyFileFullPath;
    }

    /**
     * Get the value of the specified property name from the properties file.
     *
     * @param propertyName
     * @return property value or null if not found
     * @throws IOException if there is a problem loading the properties file
     */
    public String getProperty(String propertyName) throws IOException {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            File file = new File(this.propertyFileFullPath);
            input = new FileInputStream(file);

            // load a properties file
            prop.load(input);
            return prop.getProperty(propertyName);

        } catch (IOException ex) {
            //ex.printStackTrace();
            throw ex;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
