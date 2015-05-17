package com.kostya.weightcheckadmin;

import android.database.CharArrayBuffer;
import android.database.Cursor;
import com.google.gdata.client.batch.BatchInterruptedException;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.ILink;
import com.google.gdata.data.Link;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.batch.BatchOperationType;
import com.google.gdata.data.batch.BatchUtils;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.lang.*;

/*
 * Created by Kostya on 02.03.14.
 */
class GoogleSpreadsheets {
    private final String username;
    private final String password;
    private final SpreadsheetService spreadsheetService;
    //private List<SpreadsheetEntry> spreadsheets;
    private List<WorksheetEntry> worksheets;
    private SpreadsheetEntry spreadsheetEntry;
    private String spreadsheetName = "";
    /* The number of rows to fill in the destination workbook */
    //private static final int MAX_ROWS = 75;

    /**
     * The number of columns to fill in the destination workbook
     */
    //private static final int MAX_COLS = 5;

    GoogleSpreadsheets(String user, String pass, String spreadsheetName) {
        username = user;
        password = pass;
        this.spreadsheetName = spreadsheetName;

        spreadsheetService = new SpreadsheetService("WeightScale");
        spreadsheetService.setProtocolVersion(SpreadsheetService.Versions.V3);

        //spreadsheetService.setUserCredentials(username, password);
        //feed = spreadsheetService.getFeed(new URL(URL_FEED), SpreadsheetFeed.class);
        //spreadsheets = getSpreadsheets();

        //spreadsheetEntry = getSpreadsheetEntry(spreadsheetName);//Получаем таблицу по имени

        //UpdateListWorksheets(spreadsheetEntry);

    }

    public void login() throws AuthenticationException {
        // Authenticate
        spreadsheetService.setUserCredentials(username, password);
    }

    /*List<SpreadsheetEntry> getSpreadsheets() throws Exception {
        if(feed == null)
            throw new Exception("Нет записи feeds");
        return feed.getEntries();
    }*/

    public void setSpreadsheetName(String Name) {
        spreadsheetName = Name;
    }

    SpreadsheetEntry loadSheet(String nameSheet) throws Exception {

        String URL_FEED = "https://spreadsheets.google.com/feeds/spreadsheets/private/full";
        SpreadsheetFeed feed = spreadsheetService.getFeed(new URL(URL_FEED), SpreadsheetFeed.class);
        List<SpreadsheetEntry> spreadsheets = feed.getEntries();
        //spreadsheetEntry = getSpreadsheetEntry(spreadsheets,nameSheet);
        return spreadsheetEntry = getSpreadsheetEntry(spreadsheets, nameSheet);
    }

    String getSpreadsheetName() {
        return spreadsheetName;
    }

    SpreadsheetEntry getSpreadsheetEntry(List<SpreadsheetEntry> sheets, String name) throws Exception {
        if (!sheets.isEmpty())
            for (SpreadsheetEntry spreadsheet : sheets) {
                if (spreadsheet.getTitle().getPlainText().equals(name)) {
                    return spreadsheet;
                }
            }
        throw new Exception("Нет Таблицы с именем " + name);
    }

    SpreadsheetService getSpreadsheetService() {
        return spreadsheetService;
    }

    void UpdateListWorksheets() throws IOException, ServiceException {
        worksheets = spreadsheetEntry.getWorksheets();
    }

    WorksheetEntry getWorksheetEntry(String worksheet_entry) {
        for (WorksheetEntry worksheet : worksheets) {
            if (worksheet.getTitle().getPlainText().equals(worksheet_entry)) {
                return worksheet;
            }
        }
        return null;
    }

    public boolean sendFormGo(Cursor cursor, String nameWorksheet) {

        if (spreadsheetEntry == null)
            return false;

        ArrayList<CharArrayBuffer> buffer = new ArrayList<CharArrayBuffer>();

        for (int i = 0; i < cursor.getColumnCount(); i++) {
            CharArrayBuffer b = new CharArrayBuffer(1);
            cursor.copyStringToBuffer(i, b);
            buffer.add(b);
        }

        WorksheetEntry worksheetEntry = null;

        try {

            worksheetEntry = getWorksheetEntry(nameWorksheet);

            if (worksheetEntry == null) {

                worksheetEntry = addNewWorksheet(nameWorksheet, spreadsheetEntry, cursor.getColumnNames()); //добавление нового листа
                if (worksheetEntry == null)
                    return false;//todo удалить WorksheetEntry

                UpdateListWorksheets();

                //todo проверить добавлены все названия столбцов

            }

            //if(worksheetEntry.getColCount()!=cursor.getColumnCount())
            //return false; //todo
            //ListFeed listFeed = spreadsheetService.getFeed(worksheetEntry.getListFeedUrl(), ListFeed.class);

            // TODO: Choose a row more intelligently based on your app's needs.
            //ListEntry row = listFeed.getEntries().get(1);

                 /*URL cellFeedUrl= worksheetEntry.getCellFeedUrl();
                CellFeed cellFeed= null;
                try {
                    cellFeed = spreadsheetService.getFeed (cellFeedUrl,CellFeed.class);
                } catch (ServiceException e) {
                    e.printStackTrace();
                }
                CellEntry cellEntry = null;
                String name[] = cursor.getColumnNames();
                for (int i = 0;i < name.length; i++){
                    cellEntry= new CellEntry (1, i+1, name[i]);


                    CellEntry ce = null;
                    try {
                        ce = cellFeed.insert(cellEntry);
                    } catch (ServiceException e) {
                        throw new  IOException("Ошибка добавления названия столбца" +e);
                        //todo сделать что то при ошибке
                    }

                    if(ce.getCell().equals(cellEntry.getCell())){
                        try {
                            worksheetEntry.delete();
                        } catch (ServiceException e) {
                            throw new  IOException("Ошибка удаления листа" +e);
                            //todo сделать что то при ошибке
                        }
                        return false;
                    }
                }
            }*/
        } catch (ServiceException e) {
            e.printStackTrace();
        } catch (Exception ignored) {
            return false;
        }
        try {

            URL listFeedUrl = worksheetEntry.getListFeedUrl();

            ListEntry row = new ListEntry();

            for (int i = 0; i < buffer.size(); i++) {
                String str = String.copyValueOf(buffer.get(i).data, 0, buffer.get(i).sizeCopied);
                row.getCustomElements().setValueLocal(cursor.getColumnName(i), str);
            }

            /*row =*/
            spreadsheetService.insert(listFeedUrl, row);
        } catch (ServiceException e) {
            e.printStackTrace();
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    WorksheetEntry addNewWorksheet(String name, SpreadsheetEntry spreadsheet, String... columns) throws IOException {

        WorksheetEntry worksheet = new WorksheetEntry();
        worksheet.setTitle(new PlainTextConstruct(name));
        worksheet.setColCount(columns.length);
        worksheet.setRowCount(1);

        //Добавление нового листа в таблицу
        URL worksheetFeedUrl = spreadsheet.getWorksheetFeedUrl();
        try {
            worksheet = spreadsheetService.insert(worksheetFeedUrl, worksheet);
        } catch (ServiceException e) {
            throw new IOException("Ошибка добавления нового листа " + e);
            //todo сделать что то при ошибке
        }

        //Получить feed для листа
        URL cellFeedUrl = worksheet.getCellFeedUrl();
        //
        CellFeed cellFeed;
        try {
            cellFeed = spreadsheetService.getFeed(cellFeedUrl, CellFeed.class);
        } catch (ServiceException e) {
            try {
                worksheet.delete();
            } catch (ServiceException ignored) {
                return null;
            }
            return null;
        }

        // Список адресов яцеек для заполнения названием столбцов
        List<CellAddress> cellAddresses = new ArrayList<CellAddress>();
        for (int row = 1; row <= 1; ++row) {
            for (int col = 1; col <= columns.length; ++col) {
                cellAddresses.add(new CellAddress(row, col));
            }
        }

        // Prepare the update
        // getCellEntryMap is what makes the update fast.
        try {
            Map<String, CellEntry> cellEntries = getCellEntryMap(spreadsheetService, cellFeedUrl, cellAddresses);
            int ii = 0;
            CellFeed batchRequest = new CellFeed();
            for (CellAddress cellAddress : cellAddresses) {
                CellEntry batchEntry = new CellEntry(cellEntries.get(cellAddress.idString));
                batchEntry.changeInputValueLocal(columns[ii++]);
                BatchUtils.setBatchId(batchEntry, cellAddress.idString);
                BatchUtils.setBatchOperationType(batchEntry, BatchOperationType.UPDATE);
                batchRequest.getEntries().add(batchEntry);
            }

            // Submit the update
            Link batchLink = cellFeed.getLink(ILink.Rel.FEED_BATCH, ILink.Type.ATOM);
            CellFeed batchResponse = spreadsheetService.batch(new URL(batchLink.getHref()), batchRequest);


            for (CellEntry entry : batchResponse.getEntries()) {
                if (!BatchUtils.isSuccess(entry)) {
                    worksheet.delete();
                    return null;
                }
            }

        } catch (BatchInterruptedException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            try {
                worksheet.delete();
                return null;
            } catch (ServiceException ignored) {
                return null;
            }
        }

        return worksheet;
    }

    private static Map<String, CellEntry> getCellEntryMap(SpreadsheetService ssSvc, URL cellFeedUrl, List<CellAddress> cellAddresses) throws ServiceException {
        CellFeed batchRequest = new CellFeed();
        for (CellAddress cellId : cellAddresses) {
            CellEntry batchEntry = new CellEntry(cellId.row, cellId.col, cellId.idString);
            batchEntry.setId(String.format("%s/%s", cellFeedUrl.toString(), cellId.idString));
            BatchUtils.setBatchId(batchEntry, cellId.idString);
            BatchUtils.setBatchOperationType(batchEntry, BatchOperationType.QUERY);
            batchRequest.getEntries().add(batchEntry);
        }

        CellFeed queryBatchResponse = null;
        try {
            CellFeed cellFeed = ssSvc.getFeed(cellFeedUrl, CellFeed.class);
            queryBatchResponse = ssSvc.batch(new URL(cellFeed.getLink(ILink.Rel.FEED_BATCH, ILink.Type.ATOM).getHref()), batchRequest);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new ServiceException("Ошибка " + e);
        }

        Map<String, CellEntry> cellEntryMap = new HashMap<String, CellEntry>(cellAddresses.size());
        for (CellEntry entry : queryBatchResponse.getEntries()) {
            cellEntryMap.put(BatchUtils.getBatchId(entry), entry);
        }

        return cellEntryMap;
    }

    private static class CellAddress {
        public final int row;
        public final int col;
        public final String idString;

        /**
         * Constructs a CellAddress representing the specified {@code row} and
         * {@code col}.  The idString will be set in 'RnCn' notation.
         */
        public CellAddress(int row, int col) {
            this.row = row;
            this.col = col;
            idString = String.format("R%sC%s", row, col);
        }
    }

}
