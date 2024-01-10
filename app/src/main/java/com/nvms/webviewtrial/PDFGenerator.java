package com.nvms.webviewtrial;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.OutputStream;

public class PDFGenerator {
    private static final int PDF_GENERATION_REQUEST_CODE = 2364;

    public static void generatePDF(String statements ,String FileName ,Activity activity) {
        Document document = new Document();
        try {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).setPdfContent(statements);
            }

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_TITLE, FileName+".pdf");

            activity.startActivityForResult(intent, PDF_GENERATION_REQUEST_CODE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleActivityResult(int resultCode, Uri fileUri, String pdfContent, Activity activity) {
        if (resultCode == Activity.RESULT_OK) {
            try {
                OutputStream outputStream = activity.getContentResolver().openOutputStream(fileUri);
                Document document = new Document();
                PdfWriter.getInstance(document, outputStream);
                document.open();

                Gson gson = new Gson();
                JsonElement jsonElement = gson.fromJson(pdfContent, JsonElement.class);
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                Font titleFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD);
                String appName = jsonObject.get("appName").getAsString();
                Paragraph title = new Paragraph(appName, titleFont);
                title.setAlignment(Paragraph.ALIGN_CENTER);
                document.add(title);

                String userName = jsonObject.get("userName").getAsString();
                Paragraph usernameParagraph = new Paragraph("Transactions For: " + userName);
                document.add(usernameParagraph);

                Paragraph empty = new Paragraph(" ");
                document.add(empty);

                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);

                table.addCell(new PdfPCell(new Phrase("Date")));
                table.addCell(new PdfPCell(new Phrase("Item Name")));
                table.addCell(new PdfPCell(new Phrase("Item Amount")));
                table.addCell(new PdfPCell(new Phrase("Item Description")));

                JsonArray transactionsArray = jsonObject.get("transactions").getAsJsonArray();
                for (JsonElement transactionElement : transactionsArray) {
                    JsonObject transactionObject = transactionElement.getAsJsonObject();
                    table.addCell(new PdfPCell(new Phrase(transactionObject.get("date").getAsString())));
                    table.addCell(new PdfPCell(new Phrase(transactionObject.get("name").getAsString())));
                    table.addCell(new PdfPCell(new Phrase(String.valueOf(transactionObject.get("amount").getAsInt()))));
                    table.addCell(new PdfPCell(new Phrase(transactionObject.get("description").getAsString())));
                }

                document.add(table);

                Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC | Font.BOLD);
                Paragraph footer = new Paragraph(appName, footerFont);
                footer.setAlignment(Element.ALIGN_RIGHT);
                document.add(footer);


                document.close();
                assert outputStream != null;
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
