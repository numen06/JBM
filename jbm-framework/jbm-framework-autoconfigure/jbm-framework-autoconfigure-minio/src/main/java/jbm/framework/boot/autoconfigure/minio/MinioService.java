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


import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 * Service class to interact with Minio bucket. This class is register as a bean and use the properties defined in {@link MinioConfigurationProperties}.
 * All methods return an {@link MinioException} which wrap the Minio SDK exception.
 * The bucket name is provided with the one defined in the configuration properties.
 *
 * @author Jordan LEFEBURE
 */
@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfigurationProperties configurationProperties;

    /**
     * List all objects at root of the bucket
     *
     * @return List of items
     */
    public List<Item> list() {
        Iterable<Result<Item>> myObjects = minioClient.listObjects(configurationProperties.getBucket(), "", false);
        return getItems(myObjects);
    }

    /**
     * List all objects at root of the bucket
     *
     * @return List of items
     * @throws MinioException if an error occur while fetch list
     */
    public List<Item> fullList() throws MinioException {
        try {
            Iterable<Result<Item>> myObjects = minioClient.listObjects(configurationProperties.getBucket());
            return getItems(myObjects);
        } catch (XmlPullParserException e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * List all objects with the prefix given in parameter for the bucket.
     * Simulate a folder hierarchy. Objects within folders (i.e. all objects which match the pattern {@code {prefix}/{objectName}/...}) are not returned
     *
     * @param path Prefix of seeked list of object
     * @return List of items
     */
    public List<Item> list(Path path) {

        Iterable<Result<Item>> myObjects = minioClient.listObjects(configurationProperties.getBucket(), path.toString(), false);
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
    public List<Item> getFullList(Path path) throws MinioException {
        try {
            Iterable<Result<Item>> myObjects = minioClient.listObjects(configurationProperties.getBucket(), path.toString());
            return getItems(myObjects);
        } catch (XmlPullParserException e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
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
                    } catch (InvalidBucketNameException |
                            NoSuchAlgorithmException |
                            InsufficientDataException |
                            IOException |
                            InvalidKeyException |
                            NoResponseException |
                            XmlPullParserException |
                            ErrorResponseException |
                            InternalException e) {
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
     * @throws MinioException if an error occur while fetch object
     */
    public InputStream get(Path path) throws MinioException {
        try {
            return minioClient.getObject(configurationProperties.getBucket(), path.toString());
        } catch (XmlPullParserException | InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException | InvalidKeyException | NoResponseException | ErrorResponseException | InternalException | InvalidArgumentException e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Get metadata of an object from Minio
     *
     * @param path Path with prefix to the object. Object name must be included.
     * @return Metadata of the  object
     * @throws MinioException if an error occur while fetching object metadatas
     */
    public ObjectStat getMetadata(Path path) throws MinioException {
        try {
            return minioClient.statObject(configurationProperties.getBucket(), path.toString());
        } catch (XmlPullParserException | InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException | InvalidKeyException | NoResponseException | ErrorResponseException | InternalException e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Get metadata for multiples objects from Minio
     *
     * @param paths Paths of all objects with prefix. Objects names must be included.
     * @return A map where all paths are keys and metadatas are values
     */
    public Map<Path, ObjectStat> getMetadata(Iterable<Path> paths) {
        return StreamSupport.stream(paths.spliterator(), false)
                .map(path -> {
                    try {
                        return new HashMap.SimpleEntry<>(path, minioClient.statObject(configurationProperties.getBucket(), path.toString()));
                    } catch (InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException | InvalidKeyException | NoResponseException | XmlPullParserException | ErrorResponseException | InternalException e) {
                        throw new MinioFetchException("Error while parsing list of objects", e);
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Get a file from Minio, and save it in the {@code fileName} file
     *
     * @param source   Path with prefix to the object. Object name must be included.
     * @param fileName Filename
     * @throws MinioException if an error occur while fetch object
     */
    public void getAndSave(Path source, String fileName) throws MinioException {
        try {
            minioClient.getObject(configurationProperties.getBucket(), source.toString(), fileName);
        } catch (XmlPullParserException | InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException | InvalidKeyException | NoResponseException | ErrorResponseException | InternalException | InvalidArgumentException e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Upload a file to Minio
     *
     * @param source      Path with prefix to the object. Object name must be included.
     * @param file        File as an inputstream
     * @param contentType MIME type for the object
     * @param headers     Additional headers to put on the file. The map MUST be mutable. All custom headers will start with 'x-amz-meta-' prefix when fetched with {@code getMetadata()} method.
     * @throws MinioException if an error occur while uploading object
     */
    public void upload(Path source, InputStream file, ContentType contentType, Map<String, String> headers) throws
            MinioException {
        try {
            minioClient.putObject(configurationProperties.getBucket(), source.toString(), file, (long) file.available(), headers, null, contentType.getMimeType());
        } catch (XmlPullParserException | InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException | InvalidKeyException | NoResponseException | ErrorResponseException | InternalException | InvalidArgumentException e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Upload a file to Minio
     *
     * @param source      Path with prefix to the object. Object name must be included.
     * @param file        File as an inputstream
     * @param contentType MIME type for the object
     * @throws MinioException if an error occur while uploading object
     */
    public void upload(Path source, InputStream file, ContentType contentType) throws
            MinioException {
        try {
            minioClient.putObject(configurationProperties.getBucket(), source.toString(), file, (long) file.available(), null, null, contentType.getMimeType());
        } catch (XmlPullParserException | InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException | InvalidKeyException | NoResponseException | ErrorResponseException | InternalException | InvalidArgumentException e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Upload a file to Minio
     *
     * @param source      Path with prefix to the object. Object name must be included.
     * @param file        File as an inputstream
     * @param contentType MIME type for the object
     * @param headers     Additional headers to put on the file. The map MUST be mutable
     * @throws MinioException if an error occur while uploading object
     */
    public void upload(Path source, InputStream file, String contentType, Map<String, String> headers) throws
            MinioException {
        try {
            minioClient.putObject(configurationProperties.getBucket(), source.toString(), file, (long) file.available(), headers, null, contentType);
        } catch (XmlPullParserException | InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException | InvalidKeyException | NoResponseException | ErrorResponseException | InternalException | InvalidArgumentException e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

    /**
     * Upload a file to Minio
     *
     * @param source      Path with prefix to the object. Object name must be included.
     * @param file        File as an inputstream
     * @param contentType MIME type for the object
     * @throws MinioException if an error occur while uploading object
     */
    public void upload(Path source, InputStream file, String contentType) throws
            MinioException {
        try {
            minioClient.putObject(configurationProperties.getBucket(), source.toString(), file, (long) file.available(), null, null, contentType);
        } catch (XmlPullParserException | InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException | InvalidKeyException | NoResponseException | ErrorResponseException | InternalException | InvalidArgumentException e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }


    /**
     * Remove a file to Minio
     *
     * @param source Path with prefix to the object. Object name must be included.
     * @throws MinioException if an error occur while removing object
     */
    public void remove(Path source) throws MinioException {
        try {
            minioClient.removeObject(configurationProperties.getBucket(), source.toString());
        } catch (XmlPullParserException | InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException | InvalidKeyException | NoResponseException | ErrorResponseException | InternalException | InvalidArgumentException e) {
            throw new MinioException("Error while fetching files in Minio", e);
        }
    }

}
