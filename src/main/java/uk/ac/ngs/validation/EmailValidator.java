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

package uk.ac.ngs.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
/**
 * Validate emails. 
 * @author David Meredith 
 */
public class EmailValidator {
 
	private final Pattern pattern;
	private Matcher matcher;
 
	private static final String EMAIL_PATTERN = 
		"^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
 
	public EmailValidator() {
		pattern = Pattern.compile(EMAIL_PATTERN);
	}
 
    /**
     * Validate the given email. 
     * @param email
     * @return true if valid otherwise false 
     */
	public boolean validate(final String email) {
        if(email == null) return false; 
		matcher = pattern.matcher(email);
		return matcher.matches();
	}

    /*
    Regex explanation: 
    from: http://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/
    
^			#start of the line
  [_A-Za-z0-9-\\+]+	#  must start with string in the bracket [ ], must contains one or more (+)
  (			#   start of group #1
    \\.[_A-Za-z0-9-]+	#     follow by a dot "." and string in the bracket [ ], must contains one or more (+)
  )*			#   end of group #1, this group is optional (*)
    @			#     must contains a "@" symbol
     [A-Za-z0-9-]+      #       follow by string in the bracket [ ], must contains one or more (+)
      (			#         start of group #2 - first level TLD checking
       \\.[A-Za-z0-9]+  #           follow by a dot "." and string in the bracket [ ], must contains one or more (+)
      )*		#         end of group #2, this group is optional (*)
      (			#         start of group #3 - second level TLD checking
       \\.[A-Za-z]{2,}  #           follow by a dot "." and string in the bracket [ ], with minimum length of 2
      )			#         end of group #3
$			#end of the line
    */
}
