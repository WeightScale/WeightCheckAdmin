package com.kostya.weightcheckadmin.bootloader;


import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
//import com.kostya.weightcheckadmin.Utility;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 23.12.13
 * Time: 11:28
 * To change this template use File | Settings | File Templates.
 */
class AVRDevice {
    private final Context context;
    private final Handler handler;

    final String fileName;
    private int flashSize; // Size of Flash memory in bytes.
    private int eepromSize; // Size of EEPROM memory in bytes.
    private int signature0;
    private int signature1;
    private int signature2; // The three signature bytes, read from XML PartDescriptionFiles.
    private int pageSize; // Flash page size.

    public
    /* Constructor */
    AVRDevice(final String _deviceName, Context _context, Handler _handler) {
        context = _context;
        handler = _handler;
        fileName = _deviceName;
        flashSize = eepromSize = 0;
        signature0 = signature1 = signature2 = 0;
        pageSize = -1;
    }

    /* Methods */
    void readParametersFromAVRStudio() throws Exception {
        //String path = fileName;
        //StringBuilder signature;
        //String cache = "";
        Utility Util = new Utility(context);
        if (fileName != null && fileName.length() > 0) {

	        /* Parse the file for required info */
            handler.sendMessage(handler.obtainMessage(1, "Parsing '" + fileName + "'...\r\n"));

            XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();

            flashSize = Integer.parseInt(getValue(xpp, "PROG_FLASH"));
            eepromSize = Integer.parseInt(getValue(xpp, "EEPROM"));

            /*cache += "<AVRPART><MEMORY><PROG_FLASH>";
            cache += getValue( xpp,"PROG_FLASH" );
            cache += "</PROG_FLASH><EEPROM>";
            cache += getValue( xpp,"EEPROM" );
            cache += "</EEPROM>";*/

            if (exists(xpp, "BOOT_CONFIG")) {
                pageSize = Integer.parseInt(getValue(xpp, "PAGESIZE"));
                pageSize <<= 1; // We want pagesize in bytes.

                /*cache += "<BOOT_CONFIG><PAGESIZE>";
                cache += getValue(xpp, "PAGESIZE" );
                cache += "</PAGESIZE></BOOT_CONFIG>";*/
            }

            /*cache += "</MEMORY>";

            if(exists(xpp, "FUSE"))  {
                cache += "<FUSE>";
                if(exists(xpp, "EXTENDED")) {
                    cache += "<EXTENDED></EXTENDED>";
                }
                cache += "</FUSE>";
            }*/

            //StringBuilder signature = new StringBuilder(getValue(xpp, "ADDR000"));
            //signature.deleteCharAt(0); // Remove the $ character.
            //signature0 =  Util.convertHex(signature.toString());
            signature0 = Util.convertHex(new StringBuilder(getValue(xpp, "ADDR000")).deleteCharAt(0).toString());

            //signature = new StringBuilder(getValue(xpp, "ADDR001"));
            //signature.deleteCharAt(0); // Remove the $ character.
            //signature1 = Util.convertHex(signature.toString() );
            signature1 = Util.convertHex(new StringBuilder(getValue(xpp, "ADDR001")).deleteCharAt(0).toString());

            //signature = new StringBuilder(getValue(xpp, "ADDR002"));
            //signature.deleteCharAt(0); // Remove the $ character.
            //signature2 = Util.convertHex(signature.toString());
            signature2 = Util.convertHex(new StringBuilder(getValue(xpp, "ADDR002")).deleteCharAt(0).toString());

            /*cache += "<ADMIN><SIGNATURE><ADDR000>";
            cache += getValue(xpp, "ADDR000");
            cache += "</ADDR000><ADDR001>";
            cache += getValue(xpp, "ADDR001");
            cache += "</ADDR001><ADDR002>";
            cache += getValue(xpp, "ADDR002");
            cache += "</ADDR002></SIGNATURE></ADMIN></AVRPART>\r\n";*/

	        /* Save cached file to application directory */
            handler.sendMessage(handler.obtainMessage(1, "Saving cached XML parameters...\r\n"));
        }
    }

    String getValue(XmlPullParser xml, String tag) throws IOException, XmlPullParserException {
        AssetManager manager = context.getResources().getAssets();
        InputStream input = manager.open(fileName);
        xml.setInput(input, null);
        int eventType = xml.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (xml.getName().equals(tag))
                    return xml.nextText();
            }
            eventType = xml.next();
        }
        return "";
    }

    boolean exists(XmlPullParser xml, String tag) throws IOException, XmlPullParserException {
        AssetManager manager = context.getResources().getAssets();
        InputStream input = manager.open(fileName);
        xml.setInput(input, null);
        int eventType = xml.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (xml.getName().equals(tag))
                    return true;
            }
            eventType = xml.next();
        }
        return false;
    }

    int getFlashSize() {
        return flashSize;
    }

    int getEEPROMSize() {
        return eepromSize;
    }

    long getPageSize() {
        return pageSize;
    }

    long getSignature0() {
        return signature0;
    }

    long getSignature1() {
        return signature1;
    }

    long getSignature2() {
        return signature2;
    }

}
