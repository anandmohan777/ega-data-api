/*
 * Copyright 2017 ELIXIR EGA
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
package eu.elixir.ega.ebi.downloader.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * @author asenf
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "file_index_file")
public class FileIndexFile implements Serializable {

    @Id
    @Size(max = 128)
    @Column(name = "file_id", insertable = false, updatable = false, length = 128)
    private String fileId;

    @Size(max = 128)
    @Column(name = "index_file_id", insertable = false, updatable = false, length = 128)
    private String indexFileId;

    public String toString() {
        String line = "";

        line += "File ID: " + fileId + "\n" +
                "Index FIle ID: " + indexFileId;

        return line;
    }

}
