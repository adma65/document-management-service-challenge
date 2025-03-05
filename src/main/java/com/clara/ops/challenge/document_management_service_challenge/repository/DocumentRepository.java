package com.clara.ops.challenge.document_management_service_challenge.repository;

import com.clara.ops.challenge.document_management_service_challenge.entities.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

  @Query(
      "SELECT d FROM Document d WHERE "
          + "(d.userName LIKE %?1% OR ?1 IS NULL) AND "
          + "(d.documentName LIKE %?2% OR ?2 IS NULL) AND "
          + "(d.tags LIKE %?3% OR ?3 IS NULL)")
  Page<Document> findByFilters(
      String user, String documentName, String tags, PageRequest pageRequest);
}
