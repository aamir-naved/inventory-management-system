package com.inventory.product.excel;

import com.inventory.product.entity.Product;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ProductWorkbook {

    public static final String FILENAME = "products.xlsx";
    static final String PRODUCTS_SHEET = "Products";
    static final int MAX_DATA_ROWS = 2000;

    private static final DataFormatter FORMATTER = new DataFormatter();

    enum Column {
        NAME("Name", "name", "product", "productname", "item", "itemname"),
        SKU("SKU", "sku", "code", "itemcode", "productcode"),
        CATEGORY("Category", "category", "group"),
        UNIT("Unit", "unit", "uom"),
        COST_PRICE("Cost price", "costprice", "cost", "buyprice", "purchaseprice"),
        SELLING_PRICE("Selling price", "sellingprice", "price", "sellprice", "mrp", "saleprice"),
        OPENING_STOCK("Opening stock", "openingstock", "stock", "qty", "quantity"),
        LOW_STOCK_THRESHOLD("Low stock threshold", "lowstockthreshold", "lowstock", "reorderlevel"),
        BARCODE("Barcode", "barcode", "ean", "upc"),
        HSN("HSN", "hsn", "hsncode", "hsnsac", "sac"),
        GST_RATE("GST %", "gst", "gstrate", "taxrate", "tax");

        private final String header;
        private final String[] aliases;

        Column(String header, String... aliases) {
            this.header = header;
            this.aliases = aliases;
        }

        String header() {
            return header;
        }

        boolean matches(String value) {
            String normalized = normalizeHeader(value);
            if (normalized.isEmpty()) {
                return false;
            }

            for (String alias : aliases) {
                if (alias.equals(normalized)) {
                    return true;
                }
            }

            return false;
        }
    }

    record ParsedRow(
        int rowNumber,
        String name,
        String sku,
        String category,
        String unit,
        String costPrice,
        String sellingPrice,
        String openingStock,
        String lowStockThreshold,
        String barcode,
        String hsnCode,
        String gstRate
    ) {
        boolean isBlank() {
            return isBlank(name)
                && isBlank(sku)
                && isBlank(category)
                && isBlank(unit)
                && isBlank(costPrice)
                && isBlank(sellingPrice)
                && isBlank(openingStock)
                && isBlank(lowStockThreshold)
                && isBlank(barcode)
                && isBlank(hsnCode)
                && isBlank(gstRate);
        }

        private static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }

    private ProductWorkbook() {
    }

    static byte[] export(List<Product> products) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            writeHelpSheet(workbook);
            Sheet sheet = workbook.createSheet(PRODUCTS_SHEET);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle textStyle = textStyle(workbook);

            Row headerRow = sheet.createRow(0);
            Column[] columns = Column.values();
            for (int index = 0; index < columns.length; index++) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(columns[index].header());
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowIndex++);
                writeText(row, 0, product.getName(), textStyle);
                writeText(row, 1, product.getSku(), textStyle);
                writeText(row, 2, product.getCategory(), textStyle);
                writeText(row, 3, product.getUnit(), textStyle);
                writeNumber(row, 4, product.getCostPrice());
                writeNumber(row, 5, product.getSellingPrice());
                writeNumber(row, 6, product.getOpeningStock());
                writeNumber(row, 7, product.getLowStockThreshold());
                writeText(row, 8, product.getBarcode(), textStyle);
                writeText(row, 9, product.getHsnCode(), textStyle);
                writeNumber(row, 10, product.getGstRate());
            }

            for (int index = 0; index < columns.length; index++) {
                sheet.autoSizeColumn(index);
                int width = Math.min(sheet.getColumnWidth(index) + 512, 8000);
                sheet.setColumnWidth(index, Math.max(width, 3200));
            }

            sheet.createFreezePane(0, 1);
            workbook.setSheetOrder(PRODUCTS_SHEET, 0);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create the products Excel file", exception);
        }
    }

    static List<ParsedRow> parse(InputStream inputStream) {
        Workbook workbook;
        try {
            workbook = WorkbookFactory.create(inputStream);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Upload an Excel .xlsx file exported from this screen or saved from Excel.");
        }

        try (workbook) {
            Sheet sheet = workbook.getSheet(PRODUCTS_SHEET);
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            }
            if (sheet == null) {
                throw new IllegalArgumentException("The Excel file has no product rows to import.");
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("The first row must contain column headings such as Name and Unit.");
            }

            Map<Column, Integer> columnIndexes = mapHeaders(headerRow);
            if (!columnIndexes.containsKey(Column.NAME) || !columnIndexes.containsKey(Column.UNIT)) {
                throw new IllegalArgumentException("Excel must include Name and Unit columns.");
            }

            int firstDataRow = headerRow.getRowNum() + 1;
            int lastRow = sheet.getLastRowNum();
            int dataRows = Math.max(lastRow - headerRow.getRowNum(), 0);
            if (dataRows > MAX_DATA_ROWS) {
                throw new IllegalArgumentException("Excel can contain at most " + MAX_DATA_ROWS + " products.");
            }

            List<ParsedRow> rows = new ArrayList<>();
            for (int rowIndex = firstDataRow; rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                ParsedRow parsed = new ParsedRow(
                    rowIndex + 1,
                    read(row, columnIndexes.get(Column.NAME)),
                    read(row, columnIndexes.get(Column.SKU)),
                    read(row, columnIndexes.get(Column.CATEGORY)),
                    read(row, columnIndexes.get(Column.UNIT)),
                    read(row, columnIndexes.get(Column.COST_PRICE)),
                    read(row, columnIndexes.get(Column.SELLING_PRICE)),
                    read(row, columnIndexes.get(Column.OPENING_STOCK)),
                    read(row, columnIndexes.get(Column.LOW_STOCK_THRESHOLD)),
                    read(row, columnIndexes.get(Column.BARCODE)),
                    read(row, columnIndexes.get(Column.HSN)),
                    read(row, columnIndexes.get(Column.GST_RATE))
                );
                if (!parsed.isBlank()) {
                    rows.add(parsed);
                }
            }

            return rows;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read the Excel file.");
        }
    }

    private static Map<Column, Integer> mapHeaders(Row headerRow) {
        Map<Column, Integer> indexes = new EnumMap<>(Column.class);
        short lastCell = headerRow.getLastCellNum();
        for (int index = 0; index < lastCell; index++) {
            String header = read(headerRow, index);
            if (header.isBlank()) {
                continue;
            }

            for (Column column : Column.values()) {
                if (column.matches(header) && !indexes.containsKey(column)) {
                    indexes.put(column, index);
                    break;
                }
            }
        }

        return indexes;
    }

    private static String read(Row row, Integer columnIndex) {
        if (columnIndex == null) {
            return "";
        }

        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return "";
        }

        return FORMATTER.formatCellValue(cell).trim();
    }

    private static void writeHelpSheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("How to use");
        String[] lines = {
            "Use this file to add or update products in bulk.",
            "Keep the Products sheet headings. One product goes on each row.",
            "Name and Unit are required. SKU is optional but should be unique.",
            "If a row has a SKU that already exists, that product is updated.",
            "If there is no SKU, a row updates a product with the same name, or creates one.",
            "Opening stock is used only when creating a new product. Existing stock is left unchanged.",
            "Cost price, selling price, GST %, and low stock can be left blank. Blank prices become 0.",
            "Barcode and HSN are optional. GST % must be 0, 5, 12, 18, or 28.",
            "Save the file as .xlsx and upload it from the Products page."
        };

        CellStyle wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);
        wrapStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);

        for (int index = 0; index < lines.length; index++) {
            Row row = sheet.createRow(index);
            Cell cell = row.createCell(0);
            cell.setCellValue(lines[index]);
            cell.setCellStyle(wrapStyle);
            row.setHeightInPoints(22);
        }

        sheet.setColumnWidth(0, 18000);
    }

    private static CellStyle headerStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private static CellStyle textStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("@"));
        return style;
    }

    private static void writeText(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellStyle(style);
        if (value != null && !value.isBlank()) {
            cell.setCellValue(value);
        }
    }

    private static void writeNumber(Row row, int column, BigDecimal value) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }

    private static String normalizeHeader(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
