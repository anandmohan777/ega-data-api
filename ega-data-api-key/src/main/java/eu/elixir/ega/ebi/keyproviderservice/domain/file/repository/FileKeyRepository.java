/*
 * Copyright 2016 ELIXIR EGA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.elixir.ega.ebi.keyproviderservice.domain.file.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import eu.elixir.ega.ebi.keyproviderservice.domain.file.entity.FileKey;

/**
 * @author amohan
 */
public interface FileKeyRepository extends CrudRepository<FileKey, String> {

    @Cacheable(cacheNames = "byFileKeyId")
    Iterable<FileKey> findByFileId(@Param("fileId") String fileId);
    
}