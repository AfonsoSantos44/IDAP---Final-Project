package pt.isel.services.storage

/**
 * Binary object storage for image bytes (evidence photos and generated comparison images).
 *
 * Images are not stored in PostgreSQL; the relational tables only keep the object [key].
 * The default implementation is backed by S3-compatible storage (MinIO).
 */
interface ObjectStorage {
    /** Store [bytes] under [key], overwriting any existing object with the same key. */
    fun put(
        key: String,
        bytes: ByteArray,
        contentType: String,
    )

    /** Read the full object stored under [key]. */
    fun get(key: String): ByteArray

    /** Remove the object stored under [key]. No-op if it does not exist. */
    fun delete(key: String)
}

/** An image retrieved from storage, ready to be streamed back to a client. */
data class StoredImage(
    val bytes: ByteArray,
    val contentType: String,
)
