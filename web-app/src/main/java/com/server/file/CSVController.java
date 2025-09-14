package com.server.file;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.server.framework.http.FormData;
import com.server.framework.security.SecurityUtil;
import com.server.framework.builder.ApiResponseBuilder;

@RestController
@RequestMapping("/api/v1/csv")
public class CSVController {
    private static final char DELIMITER = ',';
    private static final Logger LOGGER = Logger.getLogger(CSVController.class.getName());

    @PostMapping("/parse")
    public ResponseEntity<Map<String, Object>> doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException
    {
        try
        {
            String statsId = httpServletRequest.getParameter("stats_id");
            File statsFile = new File(SecurityUtil.getUploadsPath() + "/" + statsId + "_inprocess.csv");
            statsFile = statsFile.exists() ? statsFile : new File(statsFile.getAbsolutePath().replace("_inprocess", StringUtils.EMPTY));
            FormData formData = null;
            if(StringUtils.isNotEmpty(statsId))
            {
                if(!statsFile.exists())
                {
                    Map<String, Object> errorResponse = ApiResponseBuilder.error("Invalid Stats Id", 400);
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }
            else
            {
                Map<String, FormData> formDataMap = SecurityUtil.parseMultiPartFormData(httpServletRequest);
                formData = formDataMap.get("file");
                if(!StringUtils.equals(formData.getFileData().getContentType(), "text/csv"))
                {
                    Map<String, Object> errorResponse = ApiResponseBuilder.error("Uploaded file is not in csv format", 400);
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            StringBuilder tableData = new StringBuilder("<table>\n");
            tableData.append("<thead>\n");
            tableData.append("<tr>\n");

            AtomicInteger counter = new AtomicInteger(0);
            Reader reader = StringUtils.isNotEmpty(statsId) ? new FileReader(statsFile) : new InputStreamReader(new ByteArrayInputStream(formData.getFileData().getBytes()));
            BufferedReader bufferedReader = new BufferedReader(reader);
            Pair<String, Integer> headerPair = constructHeader(bufferedReader.readLine());
            tableData.append(headerPair.getLeft());
            final int headerSize = headerPair.getValue();
            tableData.append("</thead>\n");
            tableData.append("</tr>\n");
            tableData.append("<tbody>\n");

            bufferedReader.lines().limit(50000).forEach(line -> tableData.append(constructBody(counter.incrementAndGet(), line, headerSize)));
            tableData.append("</tbody>\n");
            Map<String, Object> data = new HashMap<>();
            data.put("table_data", tableData.toString());
            data.put("total", String.valueOf(counter.get()));
            data.put("is_completed", !statsFile.getName().contains("_inprocess"));
            
            Map<String, Object> response = ApiResponseBuilder.success("CSV parsed successfully", data);
            return ResponseEntity.ok(response);

        }
        catch(Exception e)
        {
            LOGGER.log(Level.SEVERE, "Exception occurred while parsing csv", e);
            Map<String, Object> errorResponse = ApiResponseBuilder.error("Failed to parse CSV: " + e.getMessage(), 500);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    public Pair<String, Integer> constructHeader(String line)
    {
        String header = "<th>S.NO</th>\n";
        List<String> csvRow = getCSVRow(line);
        for(String columnName : csvRow)
        {
            header += "<th>" + columnName.trim() + "</th>\n";
        }
        return new ImmutablePair<>(header, csvRow.size());
    }

    public String constructBody(int count, String line, Integer headerSize)
    {
        String rowData = "<tr>\n";
        rowData += "<td>" + count + "</td>\n";
        List<String> csvRow = getCSVRow(line);
        for(String value : csvRow)
        {
            rowData += "<td>" + StringUtils.defaultIfEmpty(value, StringUtils.EMPTY).trim() + "</td>\n";
        }
        for(int i = 0; i < headerSize - csvRow.size(); i++)
        {
            rowData += "<td> </td>\n";
        }
        rowData += "</tr>\n";
        return rowData;
    }

    public List<String> getCSVRow(String line)
    {
        List<String> values = new ArrayList<>();

        if(StringUtils.isEmpty(line))
        {
            return values;
        }

        char[] lineSplit = line.toCharArray();
        boolean isQuoteEnclosed = false;
        String currentValue = null;
        for(int i = 0; i < line.length(); i++)
        {
            if(lineSplit[i] == '"' && isQuoteEnclosed)
            {
                if((i + 1 < line.length() && lineSplit[i + 1] == '"'))
                {
                    currentValue += "\"";
                    i+=1;
                    continue;
                }
                isQuoteEnclosed = false;
                if(i + 1 < line.length())
                {
                    values.add(currentValue);
                    currentValue = null;
                }
                i+=1;
            }
            else if(lineSplit[i] == '"')
            {
                isQuoteEnclosed = !isQuoteEnclosed;
            }
            else if(lineSplit[i] == DELIMITER && !isQuoteEnclosed)
            {
                if(Objects.nonNull(currentValue))
                {
                    values.add(currentValue.trim());
                }
                else
                {
                    values.add(StringUtils.EMPTY);
                }
                currentValue = null;
            }
            else
            {
                if(currentValue == null)
                {
                    currentValue = String.valueOf(lineSplit[i]);
                }
                else
                {
                    currentValue += lineSplit[i];
                }
            }
        }
        if(Objects.nonNull(currentValue))
        {
            values.add(currentValue.trim());
        }
        return values;
    }
}
