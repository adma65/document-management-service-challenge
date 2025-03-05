package com.clara.ops.challenge.document_management_service_challenge.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import lombok.Data;

@Table(schema = "document_schema", name = "document")
@Entity
@Data
public class Document {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String userName;
  private String documentName;
  private String tags;
  private String minioPath;
  private long fileSize;
  private String fileType;
  private LocalDateTime createdAt;
}
