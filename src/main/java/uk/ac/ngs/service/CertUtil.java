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

public class CertUtil {

    public enum DNAttributeType {
        CN, L, OU, O, C
    }


    public static boolean hasSameRA(String dn1, String dn2) {
        if (dn1 == null || dn2 == null) {
            return false;
        }
        if (extractDnAttribute(dn1, DNAttributeType.L).
                equals(extractDnAttribute(dn2, DNAttributeType.L))) {
            return extractDnAttribute(dn1, DNAttributeType.OU).
                    equals(extractDnAttribute(dn2, DNAttributeType.OU));
        }
        return false;
    }

    /**
     * Get the value of the specified DN attribute.
     *
     * @param dn
     * @param attribute
     * @return The value of the attribute, null if the attribute does not exist
     * or has an empty string value.
     */
    public static String extractDnAttribute(String dn, DNAttributeType attType) {
        //dn = dn.replace('/', ','); // consider host requests where CN=globusservice/host.dl.ac.uk

        dn = dn.replaceAll(", ", ",");
        String attribute = attType.name() + "=";

        int index = dn.indexOf(attribute);
        if (index == -1) return null;

        index = index + attribute.length();
        String result = dn.substring(index);
        int _index = result.indexOf(",");
        if (_index != -1) {
            result = result.substring(0, _index);
        }
        result = result.trim();
        if ("".equals(result)) return null;
        return result;
    }

}
