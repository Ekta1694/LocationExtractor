package com.example.locationextractor;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FileUtil {
    private static final String TAG = FileUtil.class.getSimpleName();
    public  static Context context;
    public  static ProgressDialog pdia;
    public  static Dialog dialog;
    public  static ProgressBar progressBar;
    public  static TextView textstat;
    public static double lat=0.0;
    public static double lng=0.0;
    public static List<Double> latlnglist=new ArrayList();
    public FileUtil(Context context)
    {
        this.context=context;
    }


    public static List<Map<Integer, Object>> readExcelNew(Context context, Uri uri,
                                                          String filePath, int addindex, int latindex, int lngindex) {

        startDownload();

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                pdia = new ProgressDialog(context);

                pdia.setIndeterminate(false);

                pdia.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

                pdia.setMessage("Importing....");

                pdia.setCancelable(false);

                pdia.setMax(100);


            }
        });

        List<Map<Integer, Object>> list = null;
        Workbook wb;
        if (filePath == null) {
            return null;
        }
        String extString;
        if (!filePath.endsWith(".xls") && !filePath.endsWith(".xlsx")) {
            Log.e(TAG, "Please select the correct Excel file");
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    pdia.dismiss();
                }
            });
            return null;
        }
        else
        { new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                pdia.show();
            }
        });
        }

        extString = filePath.substring(filePath.lastIndexOf("."));
        InputStream is;
        try {
            is = context.getContentResolver().openInputStream(uri);
            Log.i(TAG, "readExcel: " + extString);
            if (".xls".equals(extString)) {
                wb = new HSSFWorkbook(is);
            } else if (".xlsx".equals(extString)) {
                wb = new XSSFWorkbook(is);
            } else {
                wb = null;
            }
            if (wb != null) {
                // used to store data
                list = new ArrayList<>();
                // get the first sheet
                Sheet sheet = wb.getSheetAt(0);
                // get the first line header
                Row rowHeader = sheet.getRow(0);
                int cellsCount = rowHeader.getPhysicalNumberOfCells()+1;
                //store header to the map
                Map<Integer, Object> headerMap = new HashMap<>();
                for (int c = 0; c < cellsCount; c++) {
                    Object value="";
                    if(c==addindex)
                    {
                        value="Address";
                    }
                    else
                    {
                        value = getCellFormatValue(rowHeader.getCell(c));
                    }
                 //   String cellInfo = "header " + "; c:" + c + "; v:" + value;
                  //  Log.i(TAG, "readExcelNew: " + cellInfo);

                    headerMap.put(c, value);
                }
                //add  headermap to list
                list.add(headerMap);
                // get the maximum number of rows
                int rownum = sheet.getPhysicalNumberOfRows();
                // get the maximum number of columns
                int colnum = headerMap.size();
                //index starts from 1,exclude header.
                //if you want read line by line, index should from 0.
                for (int i = 1; i < rownum; i++) {
                    Row row = sheet.getRow(i);
                    //storing subcontent
                    Map<Integer, Object> itemMap = new HashMap<>();
                    if (row != null) {
                        for (int j = 0; j < colnum; j++) {

                            Object value;
                            if(j==latindex || j==lngindex)
                            {
                                 value = row.getCell(j);
                            }
                            else
                            {
                                 value = getCellFormatValue(row.getCell(j));
                            }

                            if(j==addindex)
                            {
                                value=
                                        getCompleteAddressString(row.getCell(latindex).getNumericCellValue(),row.getCell(lngindex).getNumericCellValue());
                            //    System.out.println("lat long--"+row.getCell(latindex).getNumericCellValue()+"--"+row.getCell(lngindex).getNumericCellValue());
                              //  System.out.println("val--"+value);
                                if(value!=null && stringContainsNumber(value.toString()))
                                {
                                    int iend = value.toString().indexOf(",");
                                            String firstocuurence= value.toString().substring(0,
                                                    iend);
                               //     System.out.println("first occurence-->"+firstocuurence);

                                    String rest=value.toString().substring(iend+2,
                                            value.toString().length());
                                 //   System.out.println("final str-->"+rest);

                                    if(firstocuurence.length()>0 && stringContainsNumber(firstocuurence))
                                    {
                                        value=rest;
                                    }
                                    else
                                    {
                                        value=firstocuurence+","+rest;
                                    }
                                }
                            }
                        //    System.out.println("val modified--"+value);
                           // String cellInfo = "r: " + i + "; c:" + j + "; v:" + value;
                          // Log.i(TAG, "readExcelNew: " + cellInfo);
                            itemMap.put(j, value);
                        }
                    } else {
                        break;
                    }
                    list.add(itemMap);
                    // insert in db

                    int prog=(int) (((i+1) / (float) rownum) * 100);
                    //System.out.println("progress--"+prog);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            pdia.setProgress(prog);
                        }
                    });


                    if(prog==100)
                    {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                pdia.dismiss();
                            }
                        });
                    }
                }
            }


        } catch (Exception e) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    pdia.dismiss();
                }
            });
            e.printStackTrace();
            Log.e(TAG, "readExcelNew: import error " + e);
            Toast.makeText(context, "import error " + e, Toast.LENGTH_SHORT).show();
        }

        return list;
    }


    public static List<Map<Integer, Object>> readExcelForLatLong(Context context, Uri uri,
                                                                 String filePath, int addcolindex, int latcolindex, int lngcolindex) {

       // System.out.println("index---->"+addcolindex+"--"+latcolindex+"--"+lngcolindex);
        startDownload();


        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                pdia = new ProgressDialog(context);

                pdia.setIndeterminate(false);

                pdia.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

                pdia.setMessage("Importing....");

                pdia.setCancelable(false);

                pdia.setMax(100);


            }
        });

        List<Map<Integer, Object>> list = null;
        Workbook wb;
        if (filePath == null) {
            return null;
        }
        String extString;
        if (!filePath.endsWith(".xls") && !filePath.endsWith(".xlsx")) {
            Log.e(TAG, "Please select the correct Excel file");
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    pdia.dismiss();
                }
            });
            return null;
        }
        else
        { new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                pdia.show();
            }
        });
        }

        extString = filePath.substring(filePath.lastIndexOf("."));
        InputStream is;
        try {
            is = context.getContentResolver().openInputStream(uri);
            Log.i(TAG, "readExcel: " + extString);
            if (".xls".equals(extString)) {
                wb = new HSSFWorkbook(is);
            } else if (".xlsx".equals(extString)) {
                wb = new XSSFWorkbook(is);
            } else {
                wb = null;
            }
            if (wb != null) {
                // used to store data
                list = new ArrayList<>();
                // get the first sheet
                Sheet sheet = wb.getSheetAt(0);
                // get the first line header
                Row rowHeader = sheet.getRow(0);
                int cellsCount = rowHeader.getPhysicalNumberOfCells()+2;
                //store header to the map
                Map<Integer, Object> headerMap = new HashMap<>();
                for (int c = 0; c < cellsCount; c++) {
                    Object value="";
                    if(c==latcolindex)
                    {
                        value="Latitude";
                    }
                    else if(c==lngcolindex)
                    {
                        value="Longitude";
                    }
                    else
                    {
                      value = getCellFormatValue(rowHeader.getCell(c));
                    }

                    headerMap.put(c, value);
                }
                //add  headermap to list
                list.add(headerMap);
                // get the maximum number of rows
                int rownum = sheet.getPhysicalNumberOfRows();
                // get the maximum number of columns
                int colnum = headerMap.size();
                //index starts from 1,exclude header.
                //if you want read line by line, index should from 0.
                for (int i = 1; i < rownum; i++)
                {
                    Row row = sheet.getRow(i);
                    //storing subcontent
                    Map<Integer, Object> itemMap = new HashMap<>();
                    if (row != null) {
                        List<Double> datalist=new ArrayList<>();
                        for (int j = 0; j < colnum; j++) {
                            Object value;
                            if (j == latcolindex || j == lngcolindex) {

                                 value = row.getCell(j);
                            } else
                            {
                                value = getCellFormatValue(row.getCell(j));
                            }


                            if(j==addcolindex) // tejas
                            {
                                //int iend = value.toString().indexOf(".");// tejas


                                if(!value.toString().equals(""))
                                {
                                    String firstocuurence= value.toString();//.substring(0, iend);// tejas
                                    value=firstocuurence;
                                    datalist = getLatitudefromAddress(String.valueOf(value), context);
                                }

                            }
                            if (j == latcolindex) {


                                //  value="21.1234";
                                if(datalist.size()>0)
                                {
                                    value = datalist.get(0);
                                }
                                else
                                {
                                    value="";
                                }

                            }
                            if (j == lngcolindex) {
//                                value="72.1234";
                                if(datalist.size()>0)
                                {
                                    value = datalist.get(1);
                                }
                                else
                                {
                                    value="";
                                }
                            }

                            itemMap.put(j, value);
//                            realm.beginTransaction();
//                            ExcelData excelData=new ExcelData();
//                            int slno=Integer.parseInt(row.getCell(j).toString());
//                            excelData.setSl_no(slno);
//                            excelData.setAge();
//                            realm.copyToRealmOrUpdate(excelData);
//                            realm.commitTransaction();
                            String cellInfo = "r: " + i + "; c:" + j + "; v:"+value;
                        //    Log.i(TAG, "readExcelNew: " + cellInfo);
                        }
                    } else {
                        break;
                    }
                   // System.out.println("obj--"+itemMap.values().toString());
                //    String[] values=itemMap.values().toString().substring(1,itemMap.values().toString().length()-1).split(",");

//                    int finalI = i;
//                    ((Activity) context).runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Realm.init(context);
//                            RealmConfiguration myConfig = new RealmConfiguration.Builder()
//                                    .name("excel.realm")
//                                    .build();
//
//                            realm = Realm.getInstance(myConfig);
//
//                            realm.beginTransaction();
//                            ExcelData excelData=new ExcelData();
//                            excelData.setSl_no(Integer.parseInt(values[0].trim()));
//                            excelData.setJrny_date(values[1].trim());
//                            excelData.setTrno(Integer.parseInt(values[2].trim()));
//                            excelData.setBoardingpoint(values[3].trim());
//                            excelData.setPsg_name(values[4].trim());
//                            excelData.setAge(Integer.parseInt(values[5].trim()));
//                            excelData.setGender(values[6].trim());
//                            excelData.setMobno(values[7].trim());
//                            excelData.setDestination_address(values[8].trim());
//                            excelData.setLatitude(values[9].trim());
//                            excelData.setLongitude(values[10].trim());
//                            excelData.setSavedindx(finalI);
//                            realm.copyToRealmOrUpdate(excelData);
//                            realm.commitTransaction();
//                        }
//                    });

                    list.add(itemMap);



                    int prog=(int) (((i+1) / (float) rownum) * 100);

                  //  System.out.println("progress--"+prog);

                    BackgroundNotificationService.updateNotification(prog);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            pdia.setProgress(prog);
                        }
                    });


                    if(prog==100)
                    {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                pdia.dismiss();
                                BackgroundNotificationService.onDownloadComplete(true);

                            }
                        });
                    }
                }
            }


        } catch (Exception e) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    pdia.dismiss();
                    Toast.makeText(context, "import error " + e, Toast.LENGTH_SHORT).show();
                }
            });
            e.printStackTrace();
            Log.e(TAG, "readExcelNew: import error " + e);
          ;
        }

        return list;
    }

    public static void startDownload() {

        Intent intent = new Intent(context, BackgroundNotificationService.class);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            context.startForegroundService(intent);
//        }
//        else
//        {
            context.startService(intent);
       // }

    }




    public static boolean stringContainsNumber( String s )
    {
        Pattern p = Pattern.compile( "[0-9]" );
        Matcher m = p.matcher( s );

        return m.find();
    }


    public static String getCompleteAddressString(double LATITUDE, double LONGITUDE) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
           // System.out.println("address--"+addresses);
            if (addresses != null && addresses.size()>0) {
                Address returnedAddress = addresses.get(0);
//                StringBuilder strReturnedAddress = new StringBuilder("");
//
//                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
//                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
//                }
                strAdd = returnedAddress.getAddressLine(0);
            //    Log.w("Current loction address", strAdd);
            } else {
            //    Log.w("Current loction address", "No Address returned!");
            }
        } catch (Exception e) {
            e.printStackTrace();
           // Log.w("Current loction address", "Canont get Address!");
        }
        return strAdd;
    }


    public static List<Double> getLatitudefromAddress(final String locationAddress,
                                                      final Context context) {



                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                try {
                    latlnglist.clear();
                    List addressList = geocoder.getFromLocationName(locationAddress, 1);
                    if (addressList != null && addressList.size() > 0) {
                        Address address = (Address) addressList.get(0);
                        latlnglist.add(roundAvoid(address.getLatitude(),4));
                        latlnglist.add(roundAvoid(address.getLongitude(),4));
                    }
                } catch (IOException e) {
                    Log.e("demo", "Unable to connect to Geocoder", e);
                }

        return latlnglist;
    }

    public static double roundAvoid(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }


    public static boolean writeExcelNew(Context context, List<Map<Integer, Object>> exportExcel, int latindex, int lngindex, String date) {

        boolean isdone=false;
        try {

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    pdia = new ProgressDialog(context);

                    pdia.setIndeterminate(false);

                    pdia.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

                    pdia.setMessage("Exporting....");

                    pdia.setCancelable(false);

                    pdia.setMax(100);

                    pdia.show();
                }
            });

            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName("Sheet1"));

            int colums = exportExcel.get(0).size();
            for (int i = 0; i < colums; i++) {
                //set the cell default width to 15 characters
                sheet.setColumnWidth(i, 15 * 256);
            }

            for (int i = 0; i < exportExcel.size(); i++) {
                Row row = sheet.createRow(i);
                Map<Integer, Object> integerObjectMap = exportExcel.get(i);
                for (int j = 0; j < colums; j++) {
                    Cell cell = row.createCell(j);

                    if(integerObjectMap.get(j)!=null)
                    {
                        cell.setCellValue(String.valueOf(integerObjectMap.get(j)));
                    }
                    else
                    {
                        cell.setCellValue("");
                    }


                    if(i>0)
                    {
                        if(j==latindex||j==lngindex)
                        {
                            if(integerObjectMap.get(j)!=null && !integerObjectMap.get(j).equals(""))
                            {
                                cell.setCellValue(Double.parseDouble(String.valueOf(integerObjectMap.get(j))));
                            }
                            else
                            {
                                cell.setCellValue("");
                            }
                        }
                    }
                }

                int prog=(int) (((i+1) / (float) exportExcel.size()) * 100);
             //   System.out.println("progress--"+prog);

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        pdia.setProgress(prog);
                    }
                });
            }



//            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
//            workbook.write(outputStream);
//            outputStream.flush();
//            outputStream.close();
//            FileOutputStream fos = new FileOutputStream(String.valueOf(uri));
//            workbook.write(fos);
//            fos.close();
            Calendar cal= Calendar.getInstance();
            SimpleDateFormat sdf=new SimpleDateFormat("ddMMyy_HHmm");
            String curdate=sdf.format(cal.getTime());
            String fileName = "Excel_"+curdate+".xlsx";
            System.out.println("file name---"+fileName);
            String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
            System.out.println("path-->"+extStorageDirectory+"/LocationExtractor");

            File folder = new File(extStorageDirectory, "LocationExtractor");
            folder.mkdir();
            File file = new File(folder, fileName);
            MainActivity.setPath(file.getAbsolutePath());
            try {
                file.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            try {
                FileOutputStream fileOut = new FileOutputStream(file);
                workbook.write(fileOut);
                fileOut.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "writeExcel: export successful");
            isdone=true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "writeExcel: error" + e);
            isdone=false;

         //   Toast.makeText(context, "export error" + e, Toast.LENGTH_SHORT).show();
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                pdia.dismiss();
            }
        });
        return isdone;
    }

    /**
     * get single cell data
     *
     * @param cell </>
     * @return cell
     */
    private static Object getCellFormatValue(Cell cell) {
        Object cellValue="";
        if (cell != null) {
//            System.out.println("cell value b4==>"+cell);
//            System.out.println("cell type==>"+cell.getCellType());
            // 判断cell类型


            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_BOOLEAN:
                    cellValue = cell.getBooleanCellValue();
                    break;
                case Cell.CELL_TYPE_NUMERIC: {

                    if(cell.toString().contains("-"))
                    {
                        DateFormat originalFormat = new SimpleDateFormat("dd-MMM-yyyy",
                                Locale.ENGLISH);
                        DateFormat targetFormat = new SimpleDateFormat("dd/MM/yyyy");
                        Date date = null;
                        try {
                            date = originalFormat.parse(cell.toString());
                            String formattedDate = targetFormat.format(date);  // 20120821
                            cellValue = formattedDate;
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                    }
                    else
                    {
                        if(cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                            cellValue = NumberToTextConverter.toText(cell.getNumericCellValue());
                        }
                        else {
                            cellValue = String.valueOf(cell.getNumericCellValue());


                            if (cellValue.toString().contains(".")) {
                                int iend = cellValue.toString().indexOf(".");
                                String firstocuurence = cellValue.toString().substring(0,
                                        iend);
                                cellValue = firstocuurence;
                            }
                        }
                    }

                    break;
                }
                case Cell.CELL_TYPE_FORMULA: {
                    // determine if the cell is in date format
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // Convert to date format YYYY-mm-dd
                        cellValue = cell.getDateCellValue();
                    } else {
                        // Numeric
                        cellValue = String.valueOf(cell.getNumericCellValue());
                    }
                    break;
                }
                case Cell.CELL_TYPE_STRING: {
                    cellValue = cell.getRichStringCellValue().getString();
                    break;
                }
                default:
                    cellValue = "";
            }



        } else {
            cellValue = "";
        }


  //   System.out.println("cell value==>"+cellValue);
        return cellValue;
    }


}
