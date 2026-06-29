package pt.isel.services.storage

import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream

/**
 * S3-compatible [ObjectStorage] backed by MinIO.
 *
 * The client is lazy: no network connection is made until the first operation, so the
 * Spring context (and tests) can start without a running MinIO instance. The bucket is
 * created on demand the first time an object is written.
 */
@Service
class MinioObjectStorage(
    @Value("\${idap.storage.endpoint:http://localhost:9000}")
    endpoint: String,
    @Value("\${idap.storage.access-key:minioadmin}")
    accessKey: String,
    @Value("\${idap.storage.secret-key:minioadmin}")
    secretKey: String,
    @Value("\${idap.storage.bucket:idap-images}")
    private val bucket: String,
) : ObjectStorage {
    private val client: MinioClient =
        MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build()

    @Volatile
    private var bucketReady = false

    override fun put(
        key: String,
        bytes: ByteArray,
        contentType: String,
    ) {
        ensureBucket()
        ByteArrayInputStream(bytes).use { stream ->
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(key)
                    .stream(stream, bytes.size.toLong(), -1)
                    .contentType(contentType)
                    .build(),
            )
        }
    }

    override fun get(key: String): ByteArray =
        client.getObject(
            GetObjectArgs.builder()
                .bucket(bucket)
                .`object`(key)
                .build(),
        ).use { it.readBytes() }

    override fun delete(key: String) {
        client.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucket)
                .`object`(key)
                .build(),
        )
    }

    private fun ensureBucket() {
        if (bucketReady) return
        synchronized(this) {
            if (bucketReady) return
            val exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
            }
            bucketReady = true
        }
    }
}
