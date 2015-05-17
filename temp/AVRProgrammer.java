package com.kostya.weightcheckadmin.bootloader;

import android.os.Handler;
import com.kostya.weightcheckadmin.Scales;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 23.12.13
 * Time: 16:56
 * To change this template use File | Settings | File Templates.
 */
public class AVRProgrammer {
    private final Handler handler;
    private long pagesize; // Flash page size.
    private final int MEM_PROGRESS_GRANULARITY = 256; // For use with progress indicator.

    public
    /* Constructor */
    AVRProgrammer(Handler _handler) {
        handler = _handler;
    }

    /* Methods */
    void setPagesize(long _pagesize) {
        pagesize = _pagesize;
    }

    boolean chipErase() throws Exception {
        /* Send command 'e' */
        Scales.sendByte((byte) 'e');
        /* Should return CR */
        if (Scales.getByte() != '\r')
            throw new Exception("Chip erase failed! Programmer did not return CR after 'e'-command.");
        return true; // Indicate supported command.
    }

    boolean readSignature(Integer[] sig) {
	    /* Send command 's' */
        Scales.sendByte((byte) 's');
	    /* Get actual signature */
        sig[2] = Scales.getByte();
        sig[1] = Scales.getByte();
        sig[0] = Scales.getByte();
        return true;
    }

    boolean checkSignature(long sig0, long sig1, long sig2) throws Exception {
        //long sig[] = new long[3];
        Integer[] sig = new Integer[3];
	    /* Get signature */
        readSignature(sig);
	    /* Compare signature */
        if (sig[0] != sig0 || sig[1] != sig1 || sig[2] != sig2) {
            throw new Exception("Signature does not match selected device! ");
        }
        return true; // Indicate supported command.
    }

    void writeFlashPage() throws Exception {
        Scales.sendByte((byte) 'm');

        if (Scales.getByte() != '\r')
            throw new Exception("Writing Flash page failed! " + "Programmer did not return CR after 'm'-command.");
    }

    /*boolean writeFlashByte( long address, int value ) throws Exception {
        setAddress( address >> 1 ); // Flash operations use word addresses.

	    *//* Move data if at odd address *//*
        if( (address & 0x01) == 0x01 ) // Odd address?
            value = (value << 8) | 0x00ff; // Move to high byte of one flash word.
        else
            value |= 0xff00; // Ensure no-write for high byte.

	    *//* Send low and high byte *//*
        writeFlashLowByte( (byte)(value & 0xff) );
        writeFlashHighByte( (byte)(value >> 8) );

	    *//* Issue page write *//*
        setAddress( address >> 1 ); // The address could be autoincremented.
        writeFlashPage();

        return true; // Indicate supported command.
    }*/

    /*boolean writeEEPROMByte( long address, byte value ) throws Exception {
        if( address >= 0x10000 )
            throw new Exception( "EEPROM addresses above 64k are currently not supported!" );

        setAddress( address );

	    *//* Send data *//*
        Scales.sendByte( (byte)'D' );
        Scales.sendByte( value );
        //comm->flushTX();

	    *//* Should return CR *//*
        if( Scales.getByte() != '\r' )
            throw new Exception( "Writing byte to EEPROM failed! " + "Programmer did not return CR after 'D'-command." );

        return true; // Indicate supported command.
    }*/

    boolean writeFlash(HEXFile data) throws Exception {
        int start, end; // Data address range.
        boolean autoincrement; // Bootloader supports address autoincrement?
        int address;

	    /* Check that pagesize is set */
        if (pagesize == -1)
            throw new Exception("Programmer pagesize is not set!");

	    /* Check block write support */
        Scales.sendByte((byte) 'b');

        if (Scales.getByte() == 'Y') {
            handler.sendMessage(handler.obtainMessage(1, "Using block mode...\r\n"));
            return writeFlashBlock(data); // Finished writing.
        }

	    /* Get range from HEX file */
        start = data.getRangeStart();
        end = data.getRangeEnd();

	    /* Check autoincrement support */
        Scales.sendByte((byte) 'a');

        autoincrement = Scales.getByte() == 'Y';

	    /* Set initial address */
        setAddress(start >> 1); // Flash operations use word addresses.

	    /* Need to write one odd byte first? */
        address = start;
        if ((address & 1) == 1) {
		    /* Use only high byte */
            writeFlashLowByte((byte) 0xff); // No-write in low byte.
            writeFlashHighByte(data.getData(address));
            address++;

		    /* Need to write page? */
            if ((address % pagesize) == 0 || address > end) {// Just passed page limit or no more bytes to write?

                setAddress((address - 2) >> 1); // Set to an address inside the page.
                writeFlashPage();
                setAddress(address >> 1);
            }
        }

	    /* Write words */
        while ((end - address + 1) >= 2) {// More words left?

		    /* Need to set address again? */
            if (!autoincrement)
                setAddress(address >> 1);

		    /* Write words */
            writeFlashLowByte(data.getData(address));
            writeFlashHighByte(data.getData(address + 1));
            address += 2;

            if ((address % MEM_PROGRESS_GRANULARITY) == 0)
                handler.sendMessage(handler.obtainMessage(1, "#"));

		    /* Need to write page? */
            if ((address % pagesize) == 0 || address > end) {// Just passed a page limit or no more bytes to write?

                setAddress((address - 2) >> 1); // Set to an address inside the page.
                writeFlashPage();
                setAddress(address >> 1);
            }
        }

	    /* Need to write one even byte before finished? */
        if (address == end) {
		    /* Use only low byte */
            writeFlashLowByte(data.getData(address));
            writeFlashHighByte((byte) 0xff); // No-write in high byte.
            address += 2;

		    /* Write page */
            setAddress((address - 2) >> 1); // Set to an address inside the page.
            writeFlashPage();
        }

        handler.sendMessage(handler.obtainMessage(1, "\r\n"));
        return true; // Indicate supported command.
    }

    void writeFlashHighByte(byte value) throws Exception {
        Scales.sendByte((byte) 'C');
        Scales.sendByte(value);

        if (Scales.getByte() != '\r')
            throw new Exception("Writing Flash high byte failed! " + "Programmer did not return CR after 'C'-command.");
    }

    void writeFlashLowByte(byte value) throws Exception {
        Scales.sendByte((byte) 'c');
        Scales.sendByte(value);

        if (Scales.getByte() != '\r')
            throw new Exception("Writing Flash low byte failed! " + "Programmer did not return CR after 'c'-command.");
    }

    boolean writeFlashBlock(HEXFile data) throws Exception {
        int start, end; // Data address range.
        int blocksize; // Bootloader block size.
        int bytecount;
        int address;

	    /* Get block size, assuming command 'b' just issued and 'Y' has been read */
        blocksize = (Scales.getByte() << 8) | Scales.getByte();

	    /* Get range from HEX file */
        start = data.getRangeStart();
        end = data.getRangeEnd();

	    /* Need to write one odd byte first? */
        address = start;
        if ((address & 1) == 1) {
            setAddress(address >> 1); // Flash operations use word addresses.

		    /* Use only high byte */
            writeFlashLowByte((byte) 0xff); // No-write in low byte.
            writeFlashHighByte(data.getData(address));
            address++;

		    /* Need to write page? */
            if ((address % pagesize) == 0 || address > end) {// Just passed page limit or no more bytes to write?

                setAddress((address - 2) >> 1); // Set to an address inside the page.
                writeFlashPage();
                setAddress(address >> 1);
            }
        }

	    /* Need to write from middle to end of block first? */
        if ((address % blocksize) > 0) {// In the middle of a block?

            bytecount = blocksize - (address % blocksize); // Bytes left in block.

            if ((address + bytecount - 1) > end) {// Is that past the write range?

                bytecount = end - address + 1; // Bytes left in write range.
                bytecount &= ~0x01; // Adjust to word count.
            }

            if (bytecount > 0) {
                setAddress(address >> 1); // Flash operations use word addresses.

			    /* Start Flash block write */
                Scales.sendByte((byte) 'B');
                Scales.sendByte((byte) (bytecount >> 8)); // Size, MSB first.
                Scales.sendByte((byte) bytecount);
                Scales.sendByte((byte) 'F'); // Flash memory.

                while (bytecount > 0) {

                    Scales.sendByte(data.getData(address));
                    address++;
                    bytecount--;
                }

                if (Scales.getByte() != '\r')
                    throw new Exception("Writing Flash block failed! " + "Programmer did not return CR after 'BxxF'-command.");

                handler.sendMessage(handler.obtainMessage(1, "#")); // Advance progress indicator.
            }
        }

	    /* More complete blocks to write? */
        while ((end - address + 1) >= blocksize) {
            bytecount = blocksize;

            setAddress(address >> 1); // Flash operations use word addresses.

		    /* Start Flash block write */
            Scales.sendByte((byte) 'B');
            Scales.sendByte((byte) (bytecount >> 8)); // Size, MSB first.
            Scales.sendByte((byte) bytecount);
            Scales.sendByte((byte) 'F'); // Flash memory.

            while (bytecount > 0) {
                Scales.sendByte(data.getData(address));
                address++;
                bytecount--;
            }

            if (Scales.getByte() != '\r')
                throw new Exception("Writing Flash block failed! " + "Programmer did not return CR after 'BxxF'-command.");

            handler.sendMessage(handler.obtainMessage(3, address, 0));
        }

	    /* Any bytes left in last block */
        if ((end - address + 1) >= 1) {
            bytecount = (end - address + 1); // Get bytes left to write.
            if ((bytecount & 1) == 1)
                bytecount++; // Align to next word boundary.

            setAddress(address >> 1); // Flash operations use word addresses.

		    /* Start Flash block write */
            Scales.sendByte((byte) 'B');
            Scales.sendByte((byte) (bytecount >> 8)); // Size, MSB first.
            Scales.sendByte((byte) bytecount);
            Scales.sendByte((byte) 'F'); // Flash memory.

            while (bytecount > 0) {
                if (address > end)
                    Scales.sendByte((byte) 0xff); // Don't write outside write range.
                else
                    Scales.sendByte(data.getData(address));

                address++;
                bytecount--;
            }

            if (Scales.getByte() != '\r')
                throw new Exception("Writing Flash block failed! " + "Programmer did not return CR after 'BxxF'-command.");

            handler.sendMessage(handler.obtainMessage(1, "#"));
        }

        handler.sendMessage(handler.obtainMessage(1, "\r\n"));
        return true; // Indicate supported command.
    }

    boolean readFlash(HEXFile data) throws Exception {
        long start, end; // Data address range.
        boolean autoincrement; // Bootloader supports address autoincrement?
        long address;

        if (pagesize == -1)
            throw new Exception("Programmer pagesize is not set!");

	    /* Check block read support */
        Scales.sendByte((byte) 'b');

        if (Scales.getByte() == 'Y') {
            handler.sendMessage(handler.obtainMessage(1, "Using block mode...\r\n"));
            return readFlashBlock(data); // Finished writing.
        }

	    /* Get range from HEX file */
        start = data.getRangeStart();
        end = data.getRangeEnd();

	    /* Check autoincrement support */
        Scales.sendByte((byte) 'a');

        autoincrement = Scales.getByte() == 'Y';

	    /* Set initial address */
        setAddress(start >> 1); // Flash operations use word addresses.

	    /* Need to read one odd byte first? */
        address = start;
        if ((address & 1) == 1) {
		    /* Read both, but use only high byte */
            Scales.sendByte((byte) 'R');

            data.setData(address, (byte) Scales.getByte()); // High byte.
            Scales.getByte(); // Dont use low byte.
            address++;
        }

	    /* Get words */
        while ((end - address + 1) >= 2) {
		    /* Need to set address again? */
            if (!autoincrement)
                setAddress(address >> 1);

		    /* Get words */
            Scales.sendByte((byte) 'R');

            data.setData(address + 1, (byte) Scales.getByte()); // High byte.
            data.setData(address, (byte) Scales.getByte()); // Low byte.
            address += 2;

            if ((address % MEM_PROGRESS_GRANULARITY) == 0)
                handler.sendMessage(handler.obtainMessage(1, "#"));//log("#");//Util.progress( "#" ); // Advance progress indicator.


        }

	    /* Need to read one even byte before finished? */
        if (address == end) {
		    /* Read both, but use only low byte */
            Scales.sendByte((byte) 'R');

            Scales.getByte(); // Dont use high byte.
            data.setData(address, (byte) Scales.getByte()); // Low byte.
        }

        handler.sendMessage(handler.obtainMessage(1, "\r\n"));
        return true; // Indicate supported command.
    }

    boolean readFlashBlock(HEXFile data) throws Exception {
        int start, end; // Data address range.
        int blocksize; // Bootloader block size.
        int bytecount;
        int address;

	    /* Get block size, assuming command 'b' just issued and 'Y' has been read */
        blocksize = (Scales.getByte() << 8) | Scales.getByte();

	    /* Get range from HEX file */
        start = data.getRangeStart();
        end = data.getRangeEnd();

	    /* Need to read one odd byte first? */
        address = start;
        if ((address & 1) == 1) {
            setAddress(address >> 1); // Flash operations use word addresses.

		    /* Use only high word */
            Scales.sendByte((byte) 'R');

            data.setData(address, (byte) Scales.getByte()); // High byte.
            Scales.getByte(); // Low byte.
            address++;
        }

	    /* Need to read from middle to end of block first? */
        if ((address % blocksize) > 0) { // In the middle of a block?

            bytecount = blocksize - (address % blocksize); // Bytes left in block.

            if ((address + bytecount - 1) > end) {// Is that past the read range?

                bytecount = end - address + 1; // Bytes left in read range.
                bytecount &= ~0x01; // Adjust to word count.
            }

            if (bytecount > 0) {
                setAddress(address >> 1); // Flash operations use word addresses.

			    /* Start Flash block read */
                Scales.sendByte((byte) 'g');
                Scales.sendByte((byte) (bytecount >> 8)); // Size, MSB first.
                Scales.sendByte((byte) bytecount);
                Scales.sendByte((byte) 'F'); // Flash memory.

                while (bytecount > 0) {
                    data.setData(address, (byte) Scales.getByte());
                    address++;
                    bytecount--;
                }

                handler.sendMessage(handler.obtainMessage(1, "#"));// Advance progress indicator.
            }
        }

	    /* More complete blocks to read? */
        while ((end - address + 1) >= blocksize) {
            bytecount = blocksize;

            setAddress(address >> 1); // Flash operations use word addresses.

		    /* Start Flash block read */
            Scales.sendByte((byte) 'g');
            Scales.sendByte((byte) (bytecount >> 8)); // Size, MSB first.
            Scales.sendByte((byte) bytecount);
            Scales.sendByte((byte) 'F'); // Flash memory.

            while (bytecount > 0) {
                data.setData(address, (byte) Scales.getByte());
                address++;
                bytecount--;
            }
            handler.sendMessage(handler.obtainMessage(3, address, 0));
        }

	    /* Any bytes left in last block */
        if ((end - address + 1) >= 1) {
            bytecount = (end - address + 1); // Get bytes left to read.
            if ((bytecount & 1) == 1)
                bytecount++; // Align to next word boundary.

            setAddress(address >> 1); // Flash operations use word addresses.

		    /* Start Flash block read */
            Scales.sendByte((byte) 'g');
            Scales.sendByte((byte) (bytecount >> 8)); // Size, MSB first.
            Scales.sendByte((byte) bytecount);
            Scales.sendByte((byte) 'F'); // Flash memory.

            while (bytecount > 0) {
                if (address > end)
                    Scales.getByte(); // Don't read outside write range.
                else
                    data.setData(address, (byte) Scales.getByte());

                address++;
                bytecount--;
            }

            handler.sendMessage(handler.obtainMessage(1, "#"));
        }

        handler.sendMessage(handler.obtainMessage(1, "\r\n"));
        return true; // Indicate supported command.
    }

    /*boolean writeEEPROM( HEXFile data ) throws Exception {
        int start, end; // Data address range.
        boolean autoincrement; // Bootloader supports address autoincrement?
        int address;

	    *//* Check block write support *//*
        Scales.sendByte((byte)'b');

        if( Scales.getByte() == 'Y' ) {
            handler.sendMessage(handler.obtainMessage(1,"Using block mode...\r\n"));
            return writeEEPROMBlock( data ); // Finished writing.
        }

	    *//* Get range from HEX file *//*
        start = data.getRangeStart();
        end = data.getRangeEnd();

	    *//* Check autoincrement support *//*
        Scales.sendByte( (byte)'a' );

        autoincrement = (char) Scales.getByte() == 'Y';

	    *//* Set initial address *//*
        setAddress( start );

	    *//* Send data *//*
        address = start;
        do {
		    *//* Need to set address again? *//*
            if( !autoincrement )
                setAddress( address );

		    *//* Send byte *//*
            Scales.sendByte( (byte)'D' );
            Scales.sendByte( data.getData( address ) );

            if( Scales.getByte() != '\r' )
                throw new Exception( "Writing byte to EEPROM failed! " + "Programmer did not return CR after 'D'-command." );

            if( (address % MEM_PROGRESS_GRANULARITY) == 0 )
                handler.sendMessage(handler.obtainMessage(1,"#")); // Advance progress indicator.

            address++;
        } while( address <= end );

        handler.sendMessage(handler.obtainMessage(1,"\r\n")); // Finish progress indicator.
        return true; // Indicate supported command.
    }*/

    /*boolean writeEEPROMBlock( HEXFile data ) throws Exception {
        int start, end; // Data address range.
        int blocksize; // Bootloader block size.
        int bytecount;
        int address;

	    *//* Get block size, assuming command 'b' just issued and 'Y' has been read *//*
        blocksize = (Scales.getByte() << 8) | Scales.getByte();

	    *//* Get range from HEX file *//*
        start = data.getRangeStart();
        end = data.getRangeEnd();

	    *//* Send data *//*
        address = start;
        while( address <= end ){ // More bytes to write?

            bytecount = blocksize; // Try a full block.

            if( (address+bytecount-1) > end ){ // Is that past the write range?

                bytecount = end-address+1; // Bytes left in write range.
            }

            setAddress( address );

		    *//* Start EEPROM block write *//*
            Scales.sendByte((byte)'B');
            Scales.sendByte((byte) (bytecount >> 8)); // Size, MSB first.
            Scales.sendByte((byte) bytecount );
            Scales.sendByte( (byte)'E' ); // EEPROM memory.

            while( bytecount > 0 ) {
                Scales.sendByte( data.getData( address ) );

                address++;
                bytecount--;
            }

            if( Scales.getByte() != '\r' )
                throw new Exception( "Writing EEPROM block failed! " + "Programmer did not return CR after 'BxxE'-command." );

            handler.sendMessage(handler.obtainMessage(1,"#")); // Advance progress indicator.
        }

        handler.sendMessage(handler.obtainMessage(1,"\r\n")); // Finish progress indicator.
        return true; // Indicate supported command.
    }*/

    /*boolean readEEPROM( HEXFile data ) throws Exception {
        long start, end; // Data address range.
        boolean autoincrement; // Bootloader supports address autoincrement?
        long address;

	    *//* Check block write support *//*
        Scales.sendByte((byte)'b');

        if( Scales.getByte() == 'Y' ) {
            handler.sendMessage(handler.obtainMessage(1,"Using block mode...\r\n"));
            return readEEPROMBlock( data ); // Finished writing.
        }

	    *//* Get range from HEX file *//*
        start = data.getRangeStart();
        end = data.getRangeEnd();

	    *//* Check autoincrement support *//*
        Scales.sendByte( (byte)'a' );

        autoincrement = Scales.getByte() == 'Y';

	    *//* Set initial address *//*
        setAddress( start );

	    *//* Read data *//*
        address = start;
        do {
		    *//* Need to set address again? *//*
            if( !autoincrement )
                setAddress( address );

		    *//* Get byte *//*
            Scales.sendByte( (byte)'d' );

            data.setData( address, (byte)Scales.getByte() );

            if( (address % MEM_PROGRESS_GRANULARITY) == 0 )
                handler.sendMessage(handler.obtainMessage(1,"#"));// Advance progress indicator.

            address++;
        } while( address <= end );

        handler.sendMessage(handler.obtainMessage(1,"\r\n")); // Finish progress indicator.
        return true; // Indicate supported command.
    }*/

    /*boolean readEEPROMBlock( HEXFile data ) throws Exception {long start, end; // Data address range.
        long blocksize; // Bootloader block size.
        long bytecount;
        long address;

	    *//* Get block size, assuming command 'b' just issued and 'Y' has been read *//*
        blocksize = (Scales.getByte() << 8) | Scales.getByte();

	    *//* Get range from HEX file *//*
        start = data.getRangeStart();
        end = data.getRangeEnd();

	    *//* Read data *//*
        address = start;
        while( address <= end ){ // More bytes to read?

            bytecount = blocksize; // Try a full block.

            if( (address+bytecount-1) > end ) // Is that past the read range?
                bytecount = end-address+1; // Bytes left in read range.

            setAddress( address );

		    *//* Start EEPROM block read *//*
            Scales.sendByte( (byte)'g' );
            Scales.sendByte( (byte) (bytecount >> 8) ); // Size, MSB first.
            Scales.sendByte( (byte)bytecount  );
            Scales.sendByte( (byte)'E' ); // EEPROM memory.

            while( bytecount > 0 ) {
                data.setData(address, (byte)Scales.getByte());
                address++;
                bytecount--;
            }

            handler.sendMessage(handler.obtainMessage(1,"#"));// Advance progress indicator.
        }

        handler.sendMessage(handler.obtainMessage(1,"\r\n")); // Finish progress indicator.
        return true; // Indicate supported command.
    }*/

    /*boolean writeLockBits( byte bits ) throws Exception {
	    *//* Send command 'l' *//*
        Scales.sendByte((byte)'l');
        Scales.sendByte( (bits ) );

	    *//* Should return CR *//*
        if( Scales.getByte() != '\r' )
            throw new Exception( "Writing lock bits failed! " + "Programmer did not return CR after 'l'-command." );

        return true; // Indicate supported command.
    }*/

    /*boolean readLockBits() {
	    *//* Send command 'r' *//*
        Scales.sendByte((byte)'r');

	    *//* Get data *//*
        long bits = Scales.getByte();

        return true; // Indicate supported command.
    }*/

    /*boolean writeFuseBits() {
        return false; // Indicate unsupported command.
    }*/

    /*boolean writeExtendedFuseBits() {
        return false; // Indicate unsupported command.
    }*/

    /*boolean readFuseBits(){
        long lowfuse, highfuse;

	    *//* Send command 'N' *//*
        Scales.sendByte((byte)'N');

	    *//* Get high fuse bits *//*
        highfuse = Scales.getByte();

	    *//* Send command 'F' *//*
        Scales.sendByte( (byte)'F' );

	    *//* Get low fuse bits *//*
        lowfuse = Scales.getByte();

        return true; // Indicate supported command.
    }*/

    /*boolean readExtendedFuseBits(){
        *//* Send command 'Q' *//*
        Scales.sendByte((byte)'Q');

	    *//* Get data *//*
        long bits = Scales.getByte();

        return true; // Indicate supported command.
    }*/

    public static String readProgrammerID() {
        char[] id = new char[7]; // Reserve 7 characters.
	    /* Send 'S' command to programmer */
        Scales.sendByte((byte) 'S');
	    /* Read 7 characters */
        for (int i = 0; i < id.length; i++)
            id[i] = (char) Scales.getByte();
        return String.valueOf(id);
    }

    void setAddress(long address) throws Exception {
	    /* Set current address */
        if (address < 0x10000) {
            Scales.sendByte((byte) 'A');
            Scales.sendByte((byte) (address >> 8));
            Scales.sendByte((byte) address);
        } else {
            Scales.sendByte((byte) 'H');
            Scales.sendByte((byte) (address >> 16));
            Scales.sendByte((byte) (address >> 8));
            Scales.sendByte((byte) address);
        }

	    /* Should return CR */
        if (Scales.getByte() != '\r') {
            throw new Exception("Setting address for programming operations failed! " + "Programmer did not return CR after 'A'-command.");
        }
    }
}
