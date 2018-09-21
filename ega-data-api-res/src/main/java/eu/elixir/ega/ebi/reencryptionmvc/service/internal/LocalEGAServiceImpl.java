/*
 * Copyright 2018 ELIXIR EGA
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
package eu.elixir.ega.ebi.reencryptionmvc.service.internal;

import eu.elixir.ega.ebi.reencryptionmvc.domain.Format;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;
import eu.elixir.ega.ebi.reencryptionmvc.service.ResService;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.seekablestream.SeekableHTTPStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import io.minio.MinioClient;
import io.minio.errors.*;
import io.minio.http.Method;
import no.ifi.uio.crypt4gh.stream.Crypt4GHOutputStream;
import no.ifi.uio.crypt4gh.stream.SeekableStreamInput;
import org.apache.commons.crypto.stream.CtrCryptoOutputStream;
import org.apache.commons.crypto.stream.PositionedCryptoInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.xmlpull.v1.XmlPullParserException;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static no.ifi.uio.crypt4gh.stream.Crypt4GHInputStream.MINIMUM_BUFFER_SIZE;

/**
 * @author asenf
 */
@Service
@Profile("LocalEGA")
@EnableDiscoveryClient
public class LocalEGAServiceImpl implements ResService {

    private static final int MAX_EXPIRATION_TIME = 7 * 24 * 3600;

    @Value("${ega.ebi.aws.endpoint.url}")
    private String s3URL;

    @Value("${ega.ebi.aws.bucket:lega}")
    private String s3Bucket;

    @Value("${ega.ebi.aws.access.key}")
    private String s3Key;

    @Value("${ega.ebi.aws.access.secret}")
    private String s3Secret;

    @Autowired
    private KeyService keyService;

    @PostConstruct
    private void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public void transfer(String sourceFormat,
                         String sourceKey,
                         String sourceIV,
                         String destinationFormat,
                         String destinationKey,
                         String destinationIV,
                         String fileLocation,
                         long startCoordinate,
                         long endCoordinate,
                         long fileSize,
                         String httpAuth,
                         String id,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        InputStream inputStream;
        OutputStream outputStream;
        try {
            inputStream = getInputStream(Base64.decode(sourceKey),
                    Base64.decode(sourceIV),
                    fileLocation,
                    startCoordinate,
                    endCoordinate);
            outputStream = getOutputStream(response.getOutputStream(),
                    Format.valueOf(destinationFormat.toUpperCase()),
                    destinationKey,
                    destinationIV);
        } catch (Exception e) {
            Logger.getLogger(LocalEGAServiceImpl.class.getName()).log(Level.SEVERE, null, e);
            throw new RuntimeException(e);
        }

        response.setStatus(200);
        response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        try {
            IOUtils.copyLarge(inputStream, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            Logger.getLogger(LocalEGAServiceImpl.class.getName()).log(Level.SEVERE, null, e);
            throw new RuntimeException(e);
        }
    }

    protected InputStream getInputStream(byte[] key,
                                         byte[] iv,
                                         String fileLocation,
                                         long startCoordinate,
                                         long endCoordinate) throws IOException, InvalidPortException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, InvalidExpiresRangeException, InternalException, NoResponseException, InvalidBucketNameException, XmlPullParserException, ErrorResponseException {
        SeekableStream seekableStream;
        try {
            MinioClient minioClient = new MinioClient(s3URL, s3Key, s3Secret);
            String presignedObjectUrl = minioClient.getPresignedObjectUrl(Method.GET, s3Bucket, fileLocation, MAX_EXPIRATION_TIME, null);
            seekableStream = new SeekableHTTPStream(new URL(presignedObjectUrl));
        } catch (InvalidEndpointException e) {
            seekableStream = new SeekableFileStream(new File(fileLocation));
        }
        SeekableStreamInput seekableStreamInput = new SeekableStreamInput(seekableStream, MINIMUM_BUFFER_SIZE, 0);
        PositionedCryptoInputStream positionedStream = new PositionedCryptoInputStream(new Properties(), seekableStreamInput, key, iv, 0);
        positionedStream.seek(startCoordinate);
        return endCoordinate != 0 && endCoordinate > startCoordinate ?
                new BoundedInputStream(positionedStream, endCoordinate - startCoordinate) :
                positionedStream;
    }

    protected OutputStream getOutputStream(OutputStream outputStream, Format targetFormat, String targetKey, String targetIV) throws IOException,
            PGPException {
        switch (targetFormat) {
            case CRYPT4GH:
                return new Crypt4GHOutputStream(outputStream, keyService.getPublicKey(targetKey));
            case AES:
                return new CtrCryptoOutputStream(new Properties(), outputStream, targetKey.getBytes(), targetIV.getBytes());
            default:
                return outputStream;
        }
    }

}
