package com.krs.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public class Constants {
    public static final int REQUEST_BLUETOOTH_ENABLE_CODE = 101;
    public static final int REQUEST_LOCATION_ENABLE_CODE = 101;

    public static int SCAN_PERIOD = 10000;
    public static final int SERVERPORT = 8090;
    public static String SERVER_IP = "13.233.28.141";

    public static String txt_srvalue_sp="txt_srvalue";
    public static String txt_title1_sp="txt_title1";
    public static String txt_title2_sp="txt_title2";
    public static String txt_title3_sp="txt_title3";
    public static String txt_title4_sp="txt_title4";
    public static String txt_title5_sp="txt_title5";
    public static String txt_title6_sp="txt_title6";
    public static String txt_title7_sp="txt_title7";
    public static String edt_gross_wt_sp="edt_gross_wt";
    public static String edt_tare_wt_sp="edt_tare_wt_wt";
    public static String edt_net_wt_sp="edt_net_wt_wt";
    public static String edtLotno_sp ="edtTitle2_wt";
    public static String edtBaleno_sp ="edtTitle3_wt";
    public static String edtMaterial_sp="edtMaterial_wt";

    public static File getFile(String fileName) {
        //Saving file in external storage
        File sdCard = Environment.getExternalStorageDirectory();
        Log.d("getFile","sdCard: "+sdCard);
        File directory = new File(sdCard.getAbsolutePath() + "/superb");
        Log.d("getFile","directory: "+directory);
        //create directory if not exist
        if (!directory.isDirectory()) {
            directory.mkdirs();
        }

        //file path
        File file = new File(directory, fileName);
        Log.d("getFile","file: "+file);
        return file;
    }

    public static void exportToExcel(JSONObject mjson, String fileName, boolean isCreate) {

        File file = getFile(fileName);
        try {
            WritableSheet sheet;
            WritableWorkbook copy;
            if (isCreate) {
                WorkbookSettings wbSettings = new WorkbookSettings();
                wbSettings.setLocale(new Locale("en", "EN"));
                copy = Workbook.createWorkbook(file, wbSettings);
                sheet = copy.createSheet("Superb Insturments", 0);
            } else {
                Workbook workbook = Workbook.getWorkbook(file);
                copy = Workbook.createWorkbook(file, workbook);
                sheet = copy.getSheet(0);
            }
            try {
                sheet.addCell(new Label(0, 0, MainActivity.txt_title1.getText().toString())); // column and row
                sheet.addCell(new Label(1, 0, MainActivity.txt_title4.getText().toString()));
                sheet.addCell(new Label(2, 0, MainActivity.txt_title5.getText().toString()));
                sheet.addCell(new Label(3, 0, MainActivity.txt_title6.getText().toString()));
                sheet.addCell(new Label(4, 0, MainActivity.txt_title2.getText().toString()));
                sheet.addCell(new Label(5, 0, MainActivity.txt_title3.getText().toString()));
                sheet.addCell(new Label(6, 0, MainActivity.txt_title7.getText().toString()));
                sheet.addCell(new Label(7, 0, "Date"));
                if (mjson != null) {
                    try {
                        String srno = mjson.getString(MainActivity.txt_title1.getText().toString());
                        String lot_no = mjson.getString(MainActivity.txt_title2.getText().toString());
                        String bale_no = mjson.getString(MainActivity.txt_title3.getText().toString());
                        String gross_wt = mjson.getString(MainActivity.txt_title4.getText().toString());
                        String tare_wt = mjson.getString(MainActivity.txt_title5.getText().toString());
                        String net_wt = mjson.getString(MainActivity.txt_title6.getText().toString());
                        String material = mjson.getString(MainActivity.txt_title7.getText().toString());
                        String date = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a EEE", Locale.getDefault()).format(new Date());

                        sheet.addCell(new Label(0, Integer.parseInt(srno), srno));
                        sheet.addCell(new Label(1, Integer.parseInt(srno), gross_wt));
                        sheet.addCell(new Label(2, Integer.parseInt(srno), tare_wt));
                        sheet.addCell(new Label(3, Integer.parseInt(srno), net_wt));
                        sheet.addCell(new Label(4, Integer.parseInt(srno), lot_no));
                        sheet.addCell(new Label(5, Integer.parseInt(srno), bale_no));
                        sheet.addCell(new Label(6, Integer.parseInt(srno), material));
                        sheet.addCell(new Label(7, Integer.parseInt(srno), date));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (RowsExceededException e) {
                e.printStackTrace();
            } catch (WriteException e) {
                e.printStackTrace();
            }
            copy.write();
            try {
                copy.close();
            } catch (WriteException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        }
    }
}
