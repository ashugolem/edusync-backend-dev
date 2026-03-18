package com.project.edusync.enrollment.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single real-time progress event emitted via SSE
 * during a bulk import job.
 *
 * <p>The frontend subscribes to the SSE stream and receives one of these
 * per CSV row as it is processed.</p>
 *
 * <p>Event types:</p>
 * <ul>
 *   <li>{@code ROW_SUCCESS} – row was inserted successfully</li>
 *   <li>{@code ROW_FAILURE} – row failed validation or DB constraints</li>
 *   <li>{@code JOB_COMPLETE} – entire import finished (final summary)</li>
 *   <li>{@code JOB_FAILED}   – fatal error aborted the import</li>
 * </ul>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkImportProgressEvent {

    /** Row number in the CSV (header = 0, first data row = 1). */
    private int rowNumber;

    /** Type of event: ROW_SUCCESS | ROW_FAILURE | JOB_COMPLETE | JOB_FAILED */
    private String eventType;

    /** Human-readable identifier for the row (email or enrollmentNumber). */
    private String identifier;

    /** Error message if the row failed; null on success. */
    private String errorMessage;

    /** Running success count at the time of this event. */
    private int successCount;

    /** Running failure count at the time of this event. */
    private int failureCount;

    /** Total rows processed so far (populated on JOB_COMPLETE). */
    private int totalRows;
}

