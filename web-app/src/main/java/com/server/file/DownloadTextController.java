package com.server.file;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.server.framework.common.DateUtil;
import com.server.framework.security.SecurityUtil;
import com.server.framework.builder.ApiResponseBuilder;

@RestController
@RequestMapping("/api/v1/download/text")
public class DownloadTextController {

    private static final Logger LOGGER = Logger.getLogger(DownloadTextController.class.getName());
    @GetMapping
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        String fileName = request.getParameter("file_name");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"response.txt\"");

        FileInputStream fileInputStream = new FileInputStream(fileName);

        try(OutputStream outputStream = response.getOutputStream())
        {
            outputStream.write(SecurityUtil.readAllBytes(fileInputStream));
        }
        catch(IOException e)
        {
            LOGGER.log(java.util.logging.Level.SEVERE, "IOException in doGet", e);
        }
        finally
        {
            new File(fileName).delete();
        }
    }
    @PostMapping
    protected ResponseEntity<Map<String, Object>> doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        try {
            String text = SecurityUtil.getJSONObject(request).getString("text");

            try(ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(text.getBytes());)
            {
                String fileName = "text_" + DateUtil.getCurrentTimeInMillis() + ".txt";
                FileOutputStream fileOutputStream = new FileOutputStream(fileName);
                fileOutputStream.write(SecurityUtil.readAllBytes(byteArrayInputStream));

                Map<String, Object> data = new HashMap<>();
                data.put("file_name", fileName);
                Map<String, Object> apiResponse = ApiResponseBuilder.success("Text file created successfully", data);
                return ResponseEntity.ok(apiResponse);
            }
        }
        catch(IOException e)
        {
            LOGGER.log(java.util.logging.Level.SEVERE, "IOException in doPost", e);
            Map<String, Object> errorResponse = ApiResponseBuilder.error("Failed to create text file: " + e.getMessage(), 500);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
