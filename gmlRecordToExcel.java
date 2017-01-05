import java.io.*;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.apache.poi.xssf.usermodel.*;
import java.lang.Object;
import java.util.Observer;

/*
*   Script for updating our current record of which
*   ICON files have been converted to GML
*/

public class gmlRecordToExcel {

    /*
    *   The JSON dump txt file and the current directory under "gmls" 
    *   on Corona, as a txt file, are taken as arguments
    */
    public static void main(String [] args) {
        if (args.length > 0) {

           File jsonDump = new File(args[0]);
           File gmlDir = new File(args[1]);
           createExcelFile(jsonDump, gmlDir); 
        }
    }


    /*
    *   Creates a new excel file, saved in "excel_sheets"
    *   and named using the date it was generated, no punctuation
    *   and including seconds.
    *
    *   Designates first column as "ICON Network Names" and second as
    *   "Has GML" which will contain "1" if a GML exists
    */
    static void createExcelFile(File jsonDump, File gmlDir) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        String date = dateFormat.format(cal.getTime());
        date = date.replaceAll(" ","_");
        date = date.replaceAll(":","");
        date = date.replaceAll("/","-");
        try {
            String xFilename = "excel_sheets\\gml-record_" + date + ".xlsx";

            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet xSheet = workbook.createSheet("xSheet");
            XSSFRow rowhead = xSheet.createRow((short)0);
                rowhead.createCell(0).setCellValue("ICON Network Names");
                rowhead.createCell(1).setCellValue("Has GML");

                xSheet.setColumnWidth(0, 10000); 

            FileOutputStream fileOut = new FileOutputStream(new File(xFilename));
            workbook.write(fileOut);
            fileOut.close();
            System.out.println("Excel sheet generated.");

            writeJsonToFile(workbook, xSheet, jsonDump, xFilename, gmlDir);            
        }
        catch ( Exception ex ) {
            System.out.println(ex);
        }        
    }

    /*
    *   Reads the network names from JSON dump and writes them to excel sheet first column.
    *   Looks for the "title" tag in the JSON txt file
    */
    static void writeJsonToFile(XSSFWorkbook xFile, XSSFSheet xSheet, File jsonDump, String xFilename, File gmlDir) {
        String jsonTxtFilename = jsonDump.getAbsolutePath();
        try {
            String networkName;
            String line = null;
            int rowNo = 2;
            FileReader fileReader = new FileReader(jsonTxtFilename);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
                
            while((line = bufferedReader.readLine()) != null) {

                if(line.contains("\"title\"")) {
                    networkName = line.substring(14);
                    networkName = networkName.replaceAll("\"","");
                    networkName = networkName.replaceAll(",","");

                    XSSFRow rowhead = xSheet.createRow((short)rowNo);
                    rowhead.createCell(0).setCellValue(networkName);
                        
                    rowNo++;
                }
            }   

            FileOutputStream fileOut = new FileOutputStream(new File(xFilename));
            xFile.write(fileOut);
            fileOut.close();
            bufferedReader.close(); 
            System.out.println("Done copying JSON dump");

            crossCheckGmlDir(xFile, xSheet, xFilename, gmlDir);       
        } 
        catch(FileNotFoundException ex) {
            System.out.println(
            "Unable to open file '" + 
            jsonTxtFilename + "'");                
        }
        catch(IOException ex) {
              System.out.println(
              "Error reading file '" 
               + jsonTxtFilename + "'"); 
        }
    }

    /*
    *   Takes the GML directory txt file and cross-checks gml filenames against
    *   ICON entries to see if a GML has been created.
    *   Fills a "1" in the 2nd column of the excel sheet for ICON entries w/ GMLs
    */
    static void crossCheckGmlDir(XSSFWorkbook xFile, XSSFSheet xSheet, String xFilename, File gmlDir) {
        String gmlDirTxtFilename = gmlDir.getAbsolutePath();
        System.out.println("Cross checking with GML directory.");
        try {
            String networkName;
            String gmlFilename;
            String line = null;

            FileReader fileReader = new FileReader(gmlDirTxtFilename);
            BufferedReader bufferedReader = new BufferedReader(fileReader); 

            while((line = bufferedReader.readLine()) != null) {

                if(line.contains(".gml")) {
                    gmlFilename = line.substring(47);

                    for (int rowIndex = 2; rowIndex <= xSheet.getLastRowNum(); rowIndex++) {

                        XSSFRow row = xSheet.getRow(rowIndex);
                        XSSFCell cell = row.getCell(0);
                        networkName = cell.getStringCellValue(); 
                        //Replace all punctuation in ICON entry name to match GML filename
                        networkName = networkName.replaceAll(" ","_");
                        networkName = networkName.replaceAll(",","");
                        networkName = networkName.replaceAll("\\(","");
                        networkName = networkName.replaceAll("'","");
                        networkName = networkName.replaceAll("\\;","");
                        networkName = networkName.replaceAll("&_","");
                        networkName = networkName.replaceAll("\\)","");
                        networkName = networkName.replaceAll("\\.","");

                        if (gmlFilename.contains(networkName)) {
                            row.createCell(1).setCellValue("1");
                            break;
                        }
                    }
                }
            }   

            FileOutputStream fileOut = new FileOutputStream(new File(xFilename));
            xFile.write(fileOut);
            fileOut.close();
            bufferedReader.close(); 
            System.out.println("Done cross checking.");   

        } 
        catch(FileNotFoundException ex) {
            System.out.println(
            "Unable to open file '" + 
            gmlDirTxtFilename + "'");                
        }
        catch(IOException ex) {
              System.out.println(
              "Error reading file '" 
               + gmlDirTxtFilename + "'"); 
        }    
    }
}