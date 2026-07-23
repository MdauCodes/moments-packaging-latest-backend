package com.mdau.momentspackagingbackendjavafirstclient.backup.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadResponse;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Logical (data-only) daily backup: dumps every table in the public schema to one JSON file
 * each, zips them together with a manifest, and uploads the archive to Cloudinary.
 *
 * Deliberately data-only, not a full pg_dump-equivalent — this app's schema is fully derived
 * from JPA entities (`ddl-auto: update`), which are themselves version-controlled, so the
 * schema is reproducible from a fresh deploy. Restoring from this backup means: deploy the app
 * (schema gets created), then re-insert each table's JSON rows in the manifest's listed order
 * (parent tables before their foreign-key dependents) — no restore tooling is included here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseBackupService {

    private static final String BACKUP_FOLDER = "system-backups";
    private static final DateTimeFormatter FILENAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    /** ShedLock's own bookkeeping table — not business data. */
    private static final List<String> EXCLUDED_TABLES = List.of("shedlock");

    private final DataSource dataSource;
    private final UploadService uploadService;
    private final ObjectMapper objectMapper;
    private final Cloudinary cloudinary;

    @Value("${app.cloudinary.upload-folder}")
    private String uploadFolder;

    public record BackupResult(String cloudinaryUrl, int tableCount, long totalRows, long sizeBytes, int deletedOldBackups) {}

    public BackupResult runBackup() throws SQLException, IOException {
        List<String> tables = listTables();
        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        long totalRows = 0;

        try (Connection conn = dataSource.getConnection();
             ZipOutputStream zip = new ZipOutputStream(zipBytes)) {

            for (String table : tables) {
                List<Map<String, Object>> rows = dumpTable(conn, table);
                totalRows += rows.size();

                zip.putNextEntry(new ZipEntry(table + ".json"));
                zip.write(objectMapper.writeValueAsBytes(rows));
                zip.closeEntry();
            }

            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Africa/Nairobi"));
            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("generatedAt", now.toString());
            manifest.put("tables", tables);
            manifest.put("totalRows", totalRows);
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(objectMapper.writeValueAsBytes(manifest));
            zip.closeEntry();
        }

        byte[] archive = zipBytes.toByteArray();
        String filename = backupFilename();
        UploadResponse uploaded = uploadService.uploadBackup(archive, BACKUP_FOLDER, filename);
        int deleted = enforceRetention();

        return new BackupResult(uploaded.getUrl(), tables.size(), totalRows, archive.length, deleted);
    }

    /**
     * e.g. "backup-2026-07-23T02-00-00+03-00-Africa-Nairobi" — full date, time to the second,
     * and UTC offset + zone name spelled out, so anyone looking at the filename alone (in the
     * Cloudinary console, no need to open the archive) knows exactly when it ran.
     */
    private String backupFilename() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Africa/Nairobi"));
        String offset = now.getOffset().getId().replace(":", "-"); // "+03:00" -> "+03-00"
        return "backup-" + now.format(FILENAME_FORMAT) + offset + "-Africa-Nairobi";
    }

    /**
     * Keeps only the 2 most recent backups (the one just uploaded, plus the one immediately
     * before it) — deletes anything older. Applied every run, so it self-corrects even if a
     * run was missed for a few days (e.g. deletes 3 old ones at once rather than leaving them).
     */
    private int enforceRetention() {
        try {
            String prefix = uploadFolder + "/" + BACKUP_FOLDER + "/";
            Map<?, ?> result = cloudinary.api().resources(ObjectUtils.asMap(
                    "type", "upload",
                    "resource_type", "raw",
                    "prefix", prefix,
                    "max_results", 500
            ));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resources = (List<Map<String, Object>>) result.get("resources");
            resources.sort((a, b) -> String.valueOf(b.get("created_at")).compareTo(String.valueOf(a.get("created_at"))));

            int deleted = 0;
            for (int i = 2; i < resources.size(); i++) {
                String publicId = (String) resources.get(i).get("public_id");
                uploadService.deleteRaw(publicId);
                deleted++;
            }
            return deleted;
        } catch (Exception e) {
            log.warn("DatabaseBackupService: retention cleanup failed (backup itself still succeeded): {}", e.getMessage());
            return 0;
        }
    }

    private List<String> listTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, "public", "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                if (!EXCLUDED_TABLES.contains(name)) {
                    tables.add(name);
                }
            }
        }
        return tables;
    }

    private List<Map<String, Object>> dumpTable(Connection conn, String table) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        // Table names come from the DB's own metadata (listTables()), never user input, so
        // string-concatenating the identifier here is not an injection risk.
        try (ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM \"" + table + "\"")) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnName(i), extractValue(rs, meta, i));
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private Object extractValue(ResultSet rs, ResultSetMetaData meta, int columnIndex) throws SQLException {
        int sqlType = meta.getColumnType(columnIndex);
        String typeName = meta.getColumnTypeName(columnIndex);

        // jsonb/json come back through the PG JDBC driver as a PGobject, which Jackson has no
        // serializer for — pull the raw JSON text out directly instead.
        if ("jsonb".equals(typeName) || "json".equals(typeName)) {
            return rs.getString(columnIndex);
        }
        if (sqlType == Types.BINARY || sqlType == Types.VARBINARY || sqlType == Types.LONGVARBINARY) {
            byte[] bytes = rs.getBytes(columnIndex);
            return bytes == null ? null : Base64.getEncoder().encodeToString(bytes);
        }
        if (sqlType == Types.TIMESTAMP || sqlType == Types.TIMESTAMP_WITH_TIMEZONE) {
            java.sql.Timestamp ts = rs.getTimestamp(columnIndex);
            return ts == null ? null : ts.toInstant().toString();
        }
        if (sqlType == Types.DATE) {
            java.sql.Date d = rs.getDate(columnIndex);
            return d == null ? null : d.toLocalDate().toString();
        }
        return rs.getObject(columnIndex);
    }
}
