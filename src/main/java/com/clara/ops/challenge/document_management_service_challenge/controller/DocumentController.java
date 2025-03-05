package com.clara.ops.challenge.document_management_service_challenge.controller;

import com.clara.ops.challenge.document_management_service_challenge.entities.Document;
import com.clara.ops.challenge.document_management_service_challenge.service.DocumentService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documents")
public class DocumentController {

  @Autowired private DocumentService documentService;

  @PostMapping("/upload")
  public Document uploadDocument(
      @RequestParam("file") MultipartFile file,
      @RequestParam("user") String user,
      @RequestParam("tags") List<String> tags)
      throws Exception {
    return documentService.uploadDocument(file, user, tags);
  }

  @GetMapping("/search")
  public Page<Document> searchDocuments(
      @RequestParam(value = "user", required = false) String user,
      @RequestParam(value = "documentName", required = false) String documentName,
      @RequestParam(value = "tags", required = false) String tags,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size) {

    // Call the service method to get filtered documents with pagination
    return documentService.searchDocuments(user, documentName, tags, page, size);
  }

  @GetMapping("/download/{id}")
  public String downloadDocument(@PathVariable Long id) throws Exception {
    // Call the service method to get the pre-signed URL
    return documentService.generateDownloadUrl(id);
  }
}
