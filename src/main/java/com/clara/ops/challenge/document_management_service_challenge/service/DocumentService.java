package com.clara.ops.challenge.document_management_service_challenge.service;

import com.clara.ops.challenge.document_management_service_challenge.entities.Document;
import com.clara.ops.challenge.document_management_service_challenge.repository.DocumentRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

  @Value("${minio.bucket-name}")
  private String bucketName;

  @Autowired private MinioClient minioClient;

  @Autowired private DocumentRepository documentRepository;

  public Document uploadDocument(MultipartFile file, String user, List<String> tags)
      throws Exception {
    String documentName = file.getOriginalFilename();
    String minioPath = user + "/" + documentName;

    // Upload the file to MinIO
    try (InputStream inputStream = file.getInputStream()) {
      minioClient.putObject(
          PutObjectArgs.builder().bucket(bucketName).object(minioPath).stream(
                  inputStream, file.getSize(), -1)
              .contentType(file.getContentType())
              .build());
    }

    // Save metadata to PostgreSQL
    Document document = new Document();
    document.setUserName(user);
    document.setDocumentName(documentName);
    document.setTags(String.join(",", tags));
    document.setMinioPath(minioPath);
    document.setFileSize(file.getSize());
    document.setFileType(file.getContentType());
    document.setCreatedAt(LocalDateTime.now());

    return documentRepository.save(document);
  }

  // Method to search documents based on user, document name, and tags, with pagination support
  public Page<Document> searchDocuments(
      String user, String documentName, String tags, int page, int size) {
    // Create PageRequest for pagination
    PageRequest pageRequest = PageRequest.of(page, size);

    // Call the repository to fetch documents based on filters
    return documentRepository.findByFilters(user, documentName, tags, pageRequest);
  }

  public String generateDownloadUrl(Long documentId) {
    Document document =
        documentRepository
            .findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
    String filePath = document.getMinioPath();

    // Generate the presigned URL
    try {
      return minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(bucketName)
              .object(filePath)
              .expiry(1, TimeUnit.HOURS)
              .build());
    } catch (InvalidKeyException
        | ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidResponseException
        | NoSuchAlgorithmException
        | XmlParserException
        | ServerException
        | IllegalArgumentException
        | IOException e) {
      throw new RuntimeException();
    }
  }
}
