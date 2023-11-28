/*
 * Copyright Jordan LEFEBURE © 2019.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jbm.framework.boot.autoconfigure.minio;


import io.minio.*;
import io.minio.messages.Item;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 * Service class to interact with Minio bucket. This class is register as a bean and use the properties defined in {@link MinioConfigurationProperties}.
 * All methods return an {@link jbm.framework.boot.autoconfigure.minio.MinioException} which wrap the Minio SDK exception.
 * The bucket name is provided with the one defined in the configuration properties.
 *
 * @author Jordan LEFEBURE
 * <p>
 * <p>
 * This service adapetd with minio sdk 7.0.x
 * @author Mostafa Jalambadani
 */
@Service
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfigurationProperties configurationProperties;

    @Autowired
    public MinioService(MinioClient minioClient, MinioConfigurationProperties configurationProperties) {
        this.minioClient = minioClient;
        this.configurationProperties = configurationProperties;
    }

    /**
     * List all objects at root of the bucket
     *
     * @return List of items
     */
    public List<Item> list() {
        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(configurationProperties.getBucket())
                .prefix("")
                .recursive(false)
                .build();
        Iterable<Result<Item>> myObjects = minioClient.listObjects(args);
        return getItems(myObjects);
    }

    /**
     * List all objects at root of the bucket
     *
     * @return List of items
     */
    public List<Item> fullList() {
        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(configurationProperties.getBucket())
                .build();
        Iterable<Result<Item>> myObjects = minioClient.listObjects(args);
        return getItems(myObjects);
    }

    /**
     * List all objects with the prefix given in parameter for the bucket.
     * Simulate a folder hierarchy. Objects within folders (i.e. all objects which match the pattern {@code {prefix}/{objectName}/...}) are not returned
     *
     * @param path Prefix of seeked list of object
     * @return List of items
     */
    public List<Item> list(Path path) {
        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(configurationProperties.getBucket())
                .prefix(FilenameUtils.normalize(path.toString(), true))
                .recursive(false)
                .build();
        Iterable<Result<Item>> myObjects = minioClient.listObjects(args);
        return getItems(myObjects);
    }

    /**
     * List all objects with the prefix given in parameter for the bucket
     * <p>
     * All objects, even those which are in a folder are returned.
     *
     * @param path Prefix of seeked list of object
     * @return List of items
     */
    public List<Item> getFullList(Path path) {
        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(configurationProperties.getBucket())
                .prefix(FilenameUtils.normalize(path.toString(), true))
                .build();
        Iterable<Result<Item>> myObjects = minioClient.listObjects(args);
        return getItems(myObjects);
    }

    /**
     * Utility method which map results to items and return a list
     *
     * @param myObjects Iterable of results
     * @return List of items
     */
    private List<Item> getItems(Iterable<Result<Item>> myObjects) {
        return StreamSupport
                .stream(myObjects.spliterator(), true)
                .map(itemResult -> {
                    try {
                        return itemResult.get();
                    } catch (Exception e) {
                        throw new MinioFetchException("Error while parsing list of objects", e);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Get an object from Minio
     *
     * @param path Path with prefix to the object. Object name must be included.
     * @return The object as an InputStream
     * @throws jbm.framework.boot.autoconfigure.minio.MinioException if an error occur while fetch object
     */
    public InputStream get(Path path) throws jbm.framework.boot.autoconfigure.minio.MinioException {
        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(FilenameUtils.normalize(path.toString(), true))
                    .build();
            return minioClient.getObject(args);
        } catch (Exception e) {
            throw new jbm.framework.boot.autoconfigure.minio.MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Get metadata of an object from Minio
     *
     * @param path Path with prefix to the object. Object name must be included.
     * @return Metadata of the  object
     * @throws jbm.framework.boot.autoconfigure.minio.MinioException if an error occur while fetching object metadatas
     */
    public StatObjectResponse getMetadata(Path path) throws jbm.framework.boot.autoconfigure.minio.MinioException {
        try {
            StatObjectArgs args = StatObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(FilenameUtils.normalize(path.toString(), true))
                    .build();
            return minioClient.statObject(args);
        } catch (Exception e) {
            throw new jbm.framework.boot.autoconfigure.minio.MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Get metadata for multiples objects from Minio
     *
     * @param paths Paths of all objects with prefix. Objects names must be included.
     * @return A map where all paths are keys and metadatas are values
     */
    public Map<Path, StatObjectResponse> getMetadata(Iterable<Path> paths) {
        return StreamSupport.stream(paths.spliterator(), false)
                .map(path -> {
                    try {
                        StatObjectArgs args = StatObjectArgs.builder()
                                .bucket(configurationProperties.getBucket())
                                .object(FilenameUtils.normalize(path.toString(), true))
                                .build();
                        return new HashMap.SimpleEntry<>(path, minioClient.statObject(args));
                    } catch (Exception e) {
                        throw new MinioFetchException("Error while parsing list of objects", e);
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Get a file from Minio, and save it in the {@code fileName} file
     *
     * @param path   Path with prefix to the object. Object name must be included.
     * @param fileName Filename
     * @throws jbm.framework.boot.autoconfigure.minio.MinioException if an error occur while fetch object
     */
    public void getAndSave(Path path, String fileName) throws jbm.framework.boot.autoconfigure.minio.MinioException {
        try {
            DownloadObjectArgs args = DownloadObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(FilenameUtils.normalize(path.toString(), true))
                    .filename(fileName)
                    .build();
            minioClient.downloadObject(args);
        } catch (Exception e) {
            throw new jbm.framework.boot.autoconfigure.minio.MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Upload a file to Minio
     *
     * @param path  Path with prefix to the object. Object name must be included.
     * @param file    File as an inputstream
     * @param headers Additional headers to put on the file. The map MUST be mutable. All custom headers will start with 'x-amz-meta-' prefix when fetched with {@code getMetadata()} method.
     * @throws jbm.framework.boot.autoconfigure.minio.MinioException if an error occur while uploading object
     */
    public void upload(Path path, InputStream file, Map<String, String> headers) throws
            jbm.framework.boot.autoconfigure.minio.MinioException {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(FilenameUtils.normalize(path.toString(), true))
                    .stream(file, file.available(), -1)
                    .headers(headers)
                    .build();
            minioClient.putObject(args);
        } catch (Exception e) {
            throw new jbm.framework.boot.autoconfigure.minio.MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Upload a file to Minio
     *
     * @param path Path with prefix to the object. Object name must be included.
     * @param file   File as an inputstream
     * @throws jbm.framework.boot.autoconfigure.minio.MinioException if an error occur while uploading object
     */
    public void upload(Path path, InputStream file) throws
            jbm.framework.boot.autoconfigure.minio.MinioException {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(FilenameUtils.normalize(path.toString(), true))
                    .stream(file, file.available(), -1)
                    .build();
            minioClient.putObject(args);
        } catch (Exception e) {
            throw new jbm.framework.boot.autoconfigure.minio.MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Upload a file to Minio
     *
     * @param path      Path with prefix to the object. Object name must be included.
     * @param file        File as an inputstream
     * @param contentType MIME type for the object
     * @param headers     Additional headers to put on the file. The map MUST be mutable
     * @throws jbm.framework.boot.autoconfigure.minio.MinioException if an error occur while uploading object
     */
    public void upload(Path path, InputStream file, String contentType, Map<String, String> headers) throws
            jbm.framework.boot.autoconfigure.minio.MinioException {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(FilenameUtils.normalize(path.toString(), true))
                    .stream(file, file.available(), -1)
                    .headers(headers)
                    .contentType(contentType)
                    .build();

            minioClient.putObject(args);
        } catch (Exception e) {
            throw new jbm.framework.boot.autoconfigure.minio.MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Upload a file to Minio
     *
     * @param path      Path with prefix to the object. Object name must be included.
     * @param file        File as an inputstream
     * @param contentType MIME type for the object
     * @throws jbm.framework.boot.autoconfigure.minio.MinioException if an error occur while uploading object
     */
    public void upload(Path path, InputStream file, String contentType) throws
            jbm.framework.boot.autoconfigure.minio.MinioException {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(FilenameUtils.normalize(path.toString(), true))
                    .stream(file, file.available(), -1)
                    .contentType(contentType)
                    .build();

            minioClient.putObject(args);
        } catch (Exception e) {
            throw new jbm.framework.boot.autoconfigure.minio.MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Upload a file to Minio
     * upload file bigger than Xmx size
     *
     * @param path Path with prefix to the object. Object name must be included.
     * @param file   File as an Filename
     * @throws jbm.framework.boot.autoconfigure.minio.MinioException if an error occur while uploading object
     */
    public void upload(Path path, File file) throws
            jbm.framework.boot.autoconfigure.minio.MinioException {
        try {
            UploadObjectArgs args = UploadObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(FilenameUtils.normalize(path.toString(), true))
                    .filename(file.getAbsolutePath())
                    .build();
            minioClient.uploadObject(args);
        } catch (Exception e) {
            throw new jbm.framework.boot.autoconfigure.minio.MinioException("Error while fetching files in Minio", e);
        }
    }


    /**
     * Remove a file to Minio
     *
     * @param path Path with prefix to the object. Object name must be included.
     * @throws jbm.framework.boot.autoconfigure.minio.MinioException if an error occur while removing object
     */
    public void remove(Path path) throws jbm.framework.boot.autoconfigure.minio.MinioException {
        try {
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                    .bucket(configurationProperties.getBucket())
                    .object(FilenameUtils.normalize(path.toString(), true))
                    .build();
            minioClient.removeObject(args);
        } catch (Exception e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

}
