package com.clara.ops.challenge.document_management_service_challenge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.ops.challenge.document_management_service_challenge.entities.Document;
import com.clara.ops.challenge.document_management_service_challenge.repository.DocumentRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {

  @InjectMocks private DocumentService documentService;

  @Mock private MinioClient minioClient;

  @Mock private DocumentRepository documentRepository;

  private MockMultipartFile mockFile;
  private String user;
  private String tags;
  private List<String> tagsList;

  @BeforeEach
  public void setup() {
    mockFile =
        new MockMultipartFile(
            "file", "testDocument.pdf", "application/pdf", "Test content".getBytes());
    user = "testUser";
    tags = "tag1, tag2";
    tagsList = Arrays.asList(new String[] {"tag1", "tag2"});
    System.setProperty("minio.bucket-name", "bucket-name");

    ReflectionTestUtils.setField(documentService, "bucketName", "bucket-name");
  }

  @Test
  public void testUploadDocument() throws Exception {
    // Arrange
    Document savedDocument = new Document();
    savedDocument.setUserName(user);
    savedDocument.setDocumentName(mockFile.getOriginalFilename());
    savedDocument.setTags(String.join(",", tags));
    savedDocument.setMinioPath(user + "/" + mockFile.getOriginalFilename());
    savedDocument.setFileSize(mockFile.getSize());
    savedDocument.setFileType(mockFile.getContentType());
    savedDocument.setCreatedAt(LocalDateTime.now());

    when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

    // Act
    ResponseEntity<String> result = documentService.uploadDocument(mockFile, user, tagsList);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.OK, result.getStatusCode());
  }

  @Test
  public void testSearchDocuments() {
    String user = "testUser";
    String documentName = "TestDocument";
    String tags = "tag1,tag2";
    int page = 0;
    int size = 10;
    String sortDirection = "asc";

    // Mock Page object
    Page<Document> mockPage = mock(Page.class);

    // Mock the repository call with sorting
    when(documentRepository.findByFilters(
            user,
            documentName,
            tags,
            PageRequest.of(page, size, Sort.by(Sort.Order.asc("documentName")))))
        .thenReturn(mockPage);

    // Call the method to test
    Page<Document> result =
        documentService.searchDocuments(user, documentName, tags, page, size, sortDirection);

    // Verify that the repository method is called with the correct arguments, including sorting
    verify(documentRepository)
        .findByFilters(
            user,
            documentName,
            tags,
            PageRequest.of(page, size, Sort.by(Sort.Order.asc("documentName"))));

    // Assert that the result is not null
    assertNotNull(result, "The result should not be null");
  }

  @Test
  public void testGenerateDownloadUrl() throws Exception {
    String documentId = "1";
    Document document = new Document();
    document.setMinioPath("testUser/testDocument.pdf");

    when(documentRepository.findById(Long.valueOf(documentId))).thenReturn(Optional.of(document));
    when(minioClient.getPresignedObjectUrl(any())).thenReturn("http://localhost/download");

    String result = documentService.generateDownloadUrl(Long.valueOf(documentId));

    assertNotNull(result);
    assertEquals("http://localhost/download", result);
    verify(documentRepository, times(1)).findById(Long.valueOf(documentId));
    verify(minioClient, times(1)).getPresignedObjectUrl(any());
  }

  @Test
  public void testGenerateDownloadUrlThrowsException() {
    String documentId = "1";
    when(documentRepository.findById(Long.valueOf(documentId))).thenReturn(Optional.empty());

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> documentService.generateDownloadUrl(Long.valueOf(documentId)));
    assertEquals("Document not found", exception.getMessage());
    verify(documentRepository, times(1)).findById(Long.valueOf(documentId));
  }
}
