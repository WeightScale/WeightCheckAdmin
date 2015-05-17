package com.kostya.weightcheckadmin.bootloader;

import android.content.Context;

/*
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 23.12.13
 * Time: 14:58
 * To change this template use File | Settings | File Templates.
 */
class Utility {
    private final Context context;

    /* Constructor */
    public Utility(Context c) {
        context = c;
    }

    /* Methods */
    int convertHex(final String txt) throws Exception {
        int result = 0;
        int digit;
        int i;

        if (txt.length() == 0)
            throw new Exception("Cannot convert zero-length hex-string to number!");

        if (txt.length() > 8)
            throw new Exception("Hex conversion overflow! Too many hex digits in string.");


        for (i = 0; i < txt.length(); i++) {
            /* Convert hex digit */
            if (txt.charAt(i) >= '0' && txt.charAt(i) <= '9')
                digit = txt.charAt(i) - '0';
            else if (txt.charAt(i) >= 'a' && txt.charAt(i) <= 'f')
                digit = txt.charAt(i) - 'a' + 10;
            else if (txt.charAt(i) >= 'A' && txt.charAt(i) <= 'F')
                digit = txt.charAt(i) - 'A' + 10;
            else
                throw new Exception("Invalid hex digit found!");
		    /* Add digit as least significant 4 bits of result */
            result = (result << 4) | digit;
        }

        return result;
    }
}
