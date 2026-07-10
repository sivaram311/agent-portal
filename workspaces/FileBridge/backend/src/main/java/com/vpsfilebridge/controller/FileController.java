package com.vpsfilebridge.controller;

import com.vpsfilebridge.model.FileItem;
import com.vpsfilebridge.model.StorageStats;
import com.vpsfilebridge.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FileController {
  private final FileStorageService storage;

  public FileController(FileStorageService storage) {
    this.storage = storage;
  }

  @GetMapping("/files")
  public List<FileItem> list(@RequestParam(defaultValue = "/") String path) throws IOException {
    return storage.list(path);
  }

  @PostMapping("/files/folder")
  public Map<String, String> createFolder(@RequestBody Map<String, String> body) throws IOException {
    storage.createFolder(body.getOrDefault("path", "/"), body.get("name"));
    return Map.of("status", "created");
  }

  @PutMapping("/files/rename")
  public Map<String, String> rename(@RequestBody Map<String, String> body) throws IOException {
    storage.rename(body.get("path"), body.get("newName"));
    return Map.of("status", "renamed");
  }

  @DeleteMapping("/files")
  public Map<String, String> delete(@RequestBody Map<String, List<String>> body) throws IOException {
    storage.delete(body.getOrDefault("paths", List.of()));
    return Map.of("status", "deleted");
  }

  @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, String> upload(
      @RequestParam(defaultValue = "/") String path,
      @RequestParam("file") MultipartFile file) throws IOException {
    storage.upload(path, file);
    return Map.of("status", "uploaded", "name", file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
  }

  @GetMapping("/files/download")
  public ResponseEntity<Resource> download(@RequestParam String path) throws IOException {
    Resource resource = storage.download(path);
    String filename = resource.getFilename() == null ? "download" : resource.getFilename();
    String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    String contentType = Files.probeContentType(resource.getFile().toPath());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
        .contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType))
        .body(resource);
  }

  @PostMapping("/files/download/zip")
  public ResponseEntity<byte[]> zip(@RequestBody Map<String, List<String>> body) throws IOException {
    var baos = new java.io.ByteArrayOutputStream();
    storage.zip(body.getOrDefault("paths", List.of()), baos);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=files.zip")
        .contentType(MediaType.parseMediaType("application/zip"))
        .body(baos.toByteArray());
  }

  @GetMapping("/storage/stats")
  public StorageStats stats() throws IOException {
    return storage.stats();
  }
}
