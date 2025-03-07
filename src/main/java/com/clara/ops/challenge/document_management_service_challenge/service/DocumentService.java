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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

  @Value("${minio.bucket-name}")
  private String bucketName;

  @Autowired private MinioClient minioClient;

  @Autowired private DocumentRepository documentRepository;

  private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

  public ResponseEntity<String> uploadDocument(MultipartFile file, String user, List<String> tags) {

    if (file == null) {
      return new ResponseEntity<>("File not present", HttpStatus.BAD_REQUEST);
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      return new ResponseEntity<>("Too big to upload!, Max 50Mb", HttpStatus.BAD_REQUEST);
    }

    String documentName = file.getOriginalFilename();
    String minioPath = user + "/" + documentName;

    try {
      InputStream inputStream = file.getInputStream();
      minioClient.putObject(
          PutObjectArgs.builder().bucket(bucketName).object(minioPath).stream(
                  inputStream, file.getSize(), -1)
              .contentType(file.getContentType())
              .build());

      Document document = new Document();
      document.setUserName(user);
      document.setDocumentName(documentName);
      document.setTags(String.join(",", tags));
      document.setMinioPath(minioPath);
      document.setFileSize(file.getSize());
      document.setFileType(file.getContentType());
      document.setCreatedAt(LocalDateTime.now());

      documentRepository.save(document);

    } catch (InvalidKeyException
        | ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidResponseException
        | NoSuchAlgorithmException
        | ServerException
        | XmlParserException
        | IllegalArgumentException
        | IOException e) {
      e.printStackTrace();
    }

    return new ResponseEntity<>("The documents were uploaded successfully.", HttpStatus.OK);
  }

  public Page<Document> searchDocuments(
      String user, String documentName, String tags, int page, int size, String sort) {

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Order.asc("documentName")));

    if (!sort.isEmpty() && "desc".equalsIgnoreCase(sort))
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Order.desc("documentName")));

    return documentRepository.findByFilters(user, documentName, tags, pageRequest);
  }

  public String generateDownloadUrl(Long documentId) {
    Document document =
        documentRepository
            .findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
    String filePath = document.getMinioPath();

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
