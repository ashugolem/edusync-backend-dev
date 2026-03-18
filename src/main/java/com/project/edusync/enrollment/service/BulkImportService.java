package com.project.edusync.enrollment.service;

import com.project.edusync.enrollment.model.dto.BulkImportReportDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface BulkImportService {

    /**
     * Parses a CSV file and imports users based on the userType.
     * Emits real-time SSE progress events to the client identified by sessionId.
     *
     * @param file      The CSV file.
     * @param userType  "students" or "staff".
     * @param sessionId Unique ID for the import session; used to locate the SSE emitter.
     *                  Pass null to skip SSE emission (backwards-compatible).
     * @return A DTO containing the import results.
     * @throws IOException              if there is an issue reading the file.
     * @throws IllegalArgumentException if userType is invalid.
     */
    BulkImportReportDTO importUsers(MultipartFile file, String userType, String sessionId) throws IOException, IllegalArgumentException;
}