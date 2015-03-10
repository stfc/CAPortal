/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
