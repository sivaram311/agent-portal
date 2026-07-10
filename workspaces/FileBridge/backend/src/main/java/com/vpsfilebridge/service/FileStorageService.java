package com.vpsfilebridge.service;

import com.vpsfilebridge.model.FileItem;
import com.vpsfilebridge.model.StorageStats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileStorageService {
  private final Path root;

  public FileStorageService(@Value("${file.root-path}") String rootPath) throws IOException {
    this.root = Paths.get(rootPath).toAbsolutePath().normalize();
    Files.createDirectories(this.root);
  }

  public Path getRoot() {
    return root;
  }

  public Path resolveSafe(String relative) {
    String cleaned = relative == null ? "" : relative.replace('\\', '/').trim();
    while (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
    Path resolved = root.resolve(cleaned).normalize();
    if (!resolved.startsWith(root)) {
      throw new IllegalArgumentException("Path traversal denied");
    }
    return resolved;
  }

  public String toRelative(Path path) {
    String rel = root.relativize(path).toString().replace('\\', '/');
    return rel.isEmpty() ? "/" : "/" + rel;
  }

  public List<FileItem> list(String path) throws IOException {
    Path dir = resolveSafe(path);
    if (!Files.exists(dir)) throw new NoSuchFileException(dir.toString());
    if (!Files.isDirectory(dir)) throw new IllegalArgumentException("Not a directory");

    List<FileItem> items = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path p : stream) {
        items.add(toItem(p));
      }
    }
    items.sort(Comparator
        .comparing(FileItem::directory).reversed()
        .thenComparing(i -> i.name().toLowerCase(Locale.ROOT)));
    return items;
  }

  public FileItem toItem(Path path) throws IOException {
    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
    String name = path.getFileName().toString();
    String ext = "";
    int dot = name.lastIndexOf('.');
    if (!attrs.isDirectory() && dot > 0) ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
    return new FileItem(
        name,
        toRelative(path),
        attrs.isDirectory(),
        attrs.isDirectory() ? 0 : attrs.size(),
        attrs.lastModifiedTime().toInstant(),
        ext);
  }

  public void createFolder(String parent, String name) throws IOException {
    validateName(name);
    Path dir = resolveSafe(parent).resolve(name).normalize();
    if (!dir.startsWith(root)) throw new IllegalArgumentException("Path traversal denied");
    Files.createDirectories(dir);
  }

  public void rename(String path, String newName) throws IOException {
    validateName(newName);
    Path source = resolveSafe(path);
    if (!Files.exists(source)) throw new NoSuchFileException(source.toString());
    Path target = source.getParent().resolve(newName).normalize();
    if (!target.startsWith(root)) throw new IllegalArgumentException("Path traversal denied");
    Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
  }

  public void delete(List<String> paths) throws IOException {
    for (String p : paths) {
      Path target = resolveSafe(p);
      if (!Files.exists(target)) continue;
      if (Files.isDirectory(target)) {
        try (Stream<Path> walk = Files.walk(target)) {
          walk.sorted(Comparator.reverseOrder()).forEach(path -> {
            try { Files.deleteIfExists(path); } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
        }
      } else {
        Files.deleteIfExists(target);
      }
    }
  }

  public void upload(String parent, MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) throw new IllegalArgumentException("Empty file");
    String original = Paths.get(file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename())
        .getFileName().toString();
    validateName(original);
    Path dir = resolveSafe(parent);
    Files.createDirectories(dir);
    Path dest = dir.resolve(original).normalize();
    if (!dest.startsWith(root)) throw new IllegalArgumentException("Path traversal denied");
    try (InputStream in = file.getInputStream()) {
      Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public Resource download(String path) throws IOException {
    Path file = resolveSafe(path);
    if (!Files.exists(file) || Files.isDirectory(file)) {
      throw new NoSuchFileException(file.toString());
    }
    return new FileSystemResource(file);
  }

  public void zip(List<String> paths, OutputStream out) throws IOException {
    try (ZipOutputStream zos = new ZipOutputStream(out)) {
      for (String p : paths) {
        Path start = resolveSafe(p);
        if (!Files.exists(start)) continue;
        if (Files.isDirectory(start)) {
          try (Stream<Path> walk = Files.walk(start)) {
            for (Path file : walk.filter(Files::isRegularFile).toList()) {
              String entryName = root.relativize(file).toString().replace('\\', '/');
              zos.putNextEntry(new ZipEntry(entryName));
              Files.copy(file, zos);
              zos.closeEntry();
            }
          }
        } else {
          String entryName = start.getFileName().toString();
          zos.putNextEntry(new ZipEntry(entryName));
          Files.copy(start, zos);
          zos.closeEntry();
        }
      }
    }
  }

  public StorageStats stats() throws IOException {
    long used = 0;
    try (Stream<Path> walk = Files.walk(root)) {
      used = walk.filter(Files::isRegularFile).mapToLong(p -> {
        try { return Files.size(p); } catch (IOException e) { return 0L; }
      }).sum();
    }
    long free = Files.getFileStore(root).getUsableSpace();
    long total = Files.getFileStore(root).getTotalSpace();
    return new StorageStats(total, used, free, root.toString());
  }

  private void validateName(String name) {
    if (name == null || name.isBlank() || name.contains("/") || name.contains("\\") || name.contains("..")) {
      throw new IllegalArgumentException("Invalid name");
    }
  }
}
