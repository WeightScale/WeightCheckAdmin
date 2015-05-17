package com.kostya.weightcheckadmin.bootloader;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 23.12.13
 * Time: 18:10
 * To change this template use File | Settings | File Templates.
 */
class HEXRecord {// Intel HEX file record
    int length; // Record length in number of data bytes.
    int offset; // Offset address.
    int type; // Record type.
    byte[] data; // Optional data bytes.
}

public class HEXFile {
    private final Context context;
    private final Handler handler;
    //ActivityBootloader activityBootloader;
    private byte[] data; // Holds the data bytes.
    private int start;
    private int end; // Used data range.
    private int size; // Size of databuffer.

    void parseRecord(final String hexLine, HEXRecord recp) throws Exception {
        int checksum;
        long recordPos; // Position inside record data fields.
        Utility Util = new Utility(context);

        if (hexLine.length() < 11) // At least 11 characters.
            throw new Exception("Wrong HEX file format, missing fields! " + "Line from file was: (" + hexLine + ").");

	    /* Check format for line */
        if (hexLine.charAt(0) != ':') // Always start with colon.
            throw new Exception("Wrong HEX file format, does not start with colon! " + "Line from file was: (" + hexLine + ").");

	    /* Parse length, offset and type */
        recp.length = Util.convertHex(hexLine.substring(1, 3));
        recp.offset = Util.convertHex(hexLine.substring(3, 7));
        recp.type = Util.convertHex(hexLine.substring(7, 9));

	    /* We now know how long the record should be */
        if (hexLine.length() < (11 + recp.length * 2))
            throw new Exception("Wrong HEX file format, missing fields! " + "Line from file was: (" + hexLine + ").");

	    /* Process checksum */
        checksum = recp.length;
        checksum += ((recp.offset >> 8) & 0xff);
        checksum += (recp.offset & 0xff);
        checksum += recp.type;

	    /* Parse data fields */
        if (recp.length != 0) {

            recp.data = new byte[recp.length];
            /* Read data from record */
            for (recordPos = 0; recordPos < recp.length; recordPos++) {
                recp.data[(int) recordPos] = (byte) Util.convertHex(hexLine.substring((int) (9 + recordPos * 2), (int) (9 + recordPos * 2) + 2));
                checksum += recp.data[(int) recordPos];
            }
        }

	    /* Correct checksum? */
        checksum += Util.convertHex(hexLine.substring(9 + recp.length * 2, (9 + recp.length * 2) + 2));
        if ((checksum & 0xff) != 0) {
            throw new Exception("Wrong checksum for HEX record! " + "Line from file was: (" + hexLine + ").");
        }
    }

    /* Constructor */
    public HEXFile(int buffersize, byte value, Context con, Handler _handler) throws Exception {
        context = con;
        handler = _handler;
        if (buffersize <= 0)
            throw new Exception("Cannot have zero-size HEX buffer!");

        data = new byte[buffersize];
        size = buffersize;
        clearAll(value);
    }

    /* Methods */
    void readFile(final String _filename) throws Exception { // Read data from HEX file.

        String hexLine; // Contains one line of the HEX file.
        HEXRecord rec = new HEXRecord(); // Temp record.

        int baseAddress; // Base address for extended addressing modes.
        long dataPos; // Data position in record.

        AssetManager manager = context.getResources().getAssets();
        InputStream input = manager.open(_filename);

        if (input == null)
            throw new Exception("Error opening HEX file for input!");

        BufferedReader br = new BufferedReader(new InputStreamReader(input));
	    /* Prepare */
        baseAddress = 0;
        start = size;
        end = 0;
	    /* Parse records */
        while ((hexLine = br.readLine()) != null) {

            handler.sendMessage(handler.obtainMessage(3, end, 0));
		    /* Process record according to type */
            parseRecord(hexLine, rec);
            switch (rec.type) {
                case 0x00: // Data record ?
				    /* Copy data */
                    if (baseAddress + rec.offset + rec.length > size)
                        throw new Exception("HEX file defines data outside buffer limits! " +
                                "Make sure file does not contain data outside device " +
                                "memory limits. " +
                                "Line from file was: (" + hexLine + ").");

                    for (dataPos = 0; dataPos < rec.length; dataPos++)
                        data[(int) (baseAddress + rec.offset + dataPos)] = rec.data[(int) dataPos];

				    /* Update byte usage */
                    if (baseAddress + rec.offset < start)
                        start = baseAddress + rec.offset;

                    if (baseAddress + rec.offset + rec.length - 1 > end)
                        end = baseAddress + rec.offset + rec.length - 1;

                    break;
                case 0x02: // Extended segment address record ?
                    baseAddress = (rec.data[0] << 8) | rec.data[1];
                    baseAddress <<= 4;
                    break;
                case 0x03: // Start segment address record ?

                    break; // Ignore it, since we have no influence on execution start address.
                case 0x04: // Extended linear address record ?
                    baseAddress = (rec.data[0] << 8) | rec.data[1];
                    baseAddress <<= 16;
                    break;
                case 0x05: // Start linear address record ?

                    break; // Ignore it, since we have no influence on exectuion start address.
                case 0x01: // End of file record ?
                    br.close();
                    handler.sendMessage(handler.obtainMessage(4));
                    return;
                default: {
                    throw new Exception("Unsupported HEX record format! " + "Line from file was: (" + hexLine + ").");
                }
            }
        }

        handler.sendMessage(handler.obtainMessage(4));
	    /* We should not end up here */
        throw new Exception("Premature end of file encountered! Make sure file " + "contains an EOF-record.");
    }

    /*void writeFile( final String _filename ) throws Exception {
        FileOutputStream fos;
        HEXRecord rec = new HEXRecord(); // Temp record.

        int baseAddress; // Absolute data position.
        int offset; // Offset from base address.
        int dataPos; // Position inside data record.

        //enumStatus status;

	     *//*Attempt to create file*//*
        fos = context.openFileOutput(_filename, Context.MODE_PRIVATE);
        if( fos == null)
            throw new Exception( "Error opening HEX file for output!" );

	     *//*Prepare*//*
        //status = enumStatus._first;
        rec.data = new byte[ 16 ]; // Use only 16 byte records.

        baseAddress = start & ~0xffff; // 64K aligned address.
        offset = start & 0xffff; // Offset from the aligned address.
        dataPos = 0;

	     *//*Write first base address record to HEX file*//*
        rec.length = 2;
        rec.offset = 0;
        rec.type = 0x02;
        rec.data[1] = 0x00;
        rec.data[0] = (byte)(baseAddress >> 12); // Give 4k page index.
        writeRecord(rec ); // Write the HEX record to file.

	     *//*Write all bytes in used range*//*
        do {
		    *//* Put data into record*//*
            rec.data[dataPos] = data[(baseAddress + offset + dataPos)];
            dataPos++;

		     *//*Check if we need to write out the current data record*//*
            if( offset + dataPos >= 0x10000 ||*//*Reached 64k boundary*//*?dataPos >= 16 ||*//*Data record full*//*?baseAddress + offset + dataPos > end ) {// End of used range reached?

			     Write current data record
                rec.length = (char) dataPos;
                rec.offset = offset;
                rec.type = 0x00; // Data record.

                handler.sendMessage(handler.obtainMessage(1,"#"));// Advance progress indicator.
                writeRecord(rec );

                offset += dataPos;
                dataPos = 0;
            }

		     Check if we have passed a 64k boundary
            if( offset + dataPos >= 0x10000 ) {
			     Update address pointers
                offset -= 0x10000;
                baseAddress += 0x10000;

			     Write new base address record to HEX file
                rec.length = 2;
                rec.offset = 0;
                rec.type = 0x02;
                rec.data[0] = (byte)(baseAddress >> 12); // Give 4k page index.
                rec.data[1] = 0x00;

                writeRecord(rec ); // Write the HEX record to file.
            }
        } while( baseAddress + offset + dataPos <= end );


	     Write EOF record
        rec.length = 0;
        rec.offset = 0;
        rec.type = 0x01;

        writeRecord(rec );

        fos.close();
        handler.sendMessage(handler.obtainMessage(1,"\r\n"));// Finish progress indicator.
    } // Write data to HEX file.*/

    void setUsedRange(int _start, int _end) throws Exception {// Sets the used range.
        if (_start < 0 || _end >= size || _start > _end)
            throw new Exception("Invalid range! Start must be 0 or larger, end must be " + "inside allowed memory range.");

        start = _start;
        end = _end;
    }

    void clearAll(byte value) {// Set databuffer to this value.
        for (int i = 0; i < size; i++)
            data[i] = (byte) (value & 0xff);
    }

    /*Field getFieldFile(final String _filename) throws Exception {
        Field[] fields = R.raw.class.getFields();
        if(fields != null && fields.length > 0) {
	        *//* Search for file *//*
            int i;
            for( i = 0; i < fields.length; i++ )  {
                if(fields[i].getName().equals(_filename))
                    break;
            }

            if( i == fields.length )
                throw new Exception( "Device XML file not found in search path!" );
            return fields[i];
        }
        return null;
    }*/

    int getRangeStart() {
        return start;
    }

    int getRangeEnd() {
        return end;
    }

    byte getData(int address) throws Exception {
        if (address < 0 || address >= size)
            throw new Exception("Address outside legal range!");
        return data[address];
    }

    void setData(long address, byte value) throws Exception {
        if (address < 0 || address >= size)
            throw new Exception("Address outside legal range!");

        data[(int) address] = value;
    }

    /*long getSize() { return size; }*/
}
