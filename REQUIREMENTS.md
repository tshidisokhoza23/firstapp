
Here is the extended master implementation prompt updated to cover end-to-end Avatar Editing, Uploading, and Storage across both the Android client and the Go backend, addressing all security and performance edge cases.
Master Implementation Prompt (Updated with Avatar Editing)
# TASK SPECIFICATION: Google Auth, Progressive Profile Onboarding & Avatar Management

Implement an end-to-end authentication, progressive onboarding, and avatar customization pipeline using:
- **Client:** Android (Kotlin, Jetpack Compose, AndroidX PhotoPicker, Coil)
- **Backend:** Go (REST API, JWT auth)
- **Persistence & Storage:** PostgreSQL, Redis, and S3-compatible Object Storage (e.g., Cloudflare R2 / AWS S3 / MinIO)

---

## 1. AVATAR ARCHITECTURE & WORKFLOW

1. **Initial Fallback (Google Sign-In):**
    - On first sign-in, extract the Google profile photo (`picture` claim from the Google ID token) and set it as `users.avatar_url`.
2. **Avatar Customization (Onboarding & Profile Settings):**
    - The user can tap their avatar to change it during onboarding or anytime in profile settings.
    - The client uses the modern Android **PhotoPicker** (`ActivityResultContracts.PickVisualMedia(VisualMediaType.ImageOnly)`), eliminating the need for broad `READ_EXTERNAL_STORAGE` permissions.
3. **Upload Flow (Direct Multipart to Go API or Pre-Signed S3 URL):**
    - Client compresses and crops the image locally to a standard square (e.g., 512x512 px WebP/JPEG, max 500 KB).
    - Client uploads the image via `PUT /api/v1/user/avatar` (multipart form-data) with JWT authentication.
    - Backend performs strict MIME sniffing, strips metadata, generates a collision-free UUID object key, stores it in Object Storage, deletes the old custom avatar (if applicable), and updates `users.avatar_url`.

---

## 2. DATABASE & STORAGE SCHEMA

### PostgreSQL Updates
```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_storage_key VARCHAR(255);

-- Ensure avatar_url defaults to null or Google picture, but does not block profile completion if optional
-- If avatar is required for completion:
ALTER TABLE users DROP COLUMN is_profile_complete;
ALTER TABLE users ADD COLUMN is_profile_complete BOOLEAN GENERATED ALWAYS AS (
    display_name IS NOT NULL AND 
    username IS NOT NULL AND 
    phone_number IS NOT NULL AND
    avatar_url IS NOT NULL
) STORED;
3. SECURITY & EDGE CASE MATRIX (Avatar & Media)CategoryVulnerability / Edge CaseRequired MitigationSecurityMalicious File Upload (e.g. PHP/EXE disguised as .jpg)Do not trust Content-Type header or file extension. Sniff the first 512 bytes on the Go server using http.DetectContentType(buffer) and allow only image/jpeg, image/png, and image/webp.SecurityEXIF Geolocation / Privacy LeakRe-encode/decode the image on the server or use a strip-metadata pass before persisting to S3/R2 storage.SecurityDoS via Giant UploadsEnforce strict request body size limit via http.MaxBytesReader(w, r.Body, 5 << 20) (5 MB max).StorageOrphaned / Leaked Storage ObjectsWhen a user updates their avatar, delete the previous object key referenced in avatar_storage_key from Object Storage.CachingStale Avatar on CDN / Coil Image CacheUse immutable versioned paths (/avatars/{user_id}/{uuid}.webp) rather than overwriting a static /avatars/{user_id}.jpg.ReliabilityNetwork Interruption During UploadEnsure upload endpoint is idempotent; client rolls back local avatar preview to previous state if the upload request fails.
4. BACKEND IMPLEMENTATION REQUIREMENTS (Go)
Avatar Upload Endpoint: PUT /api/v1/user/avatar

    Authentication: Authorization: Bearer <access_token>

    Content-Type: multipart/form-data (form field: avatar)

    Execution Flow:

        Limit request payload to 5MB using http.MaxBytesReader.

        Parse multipart form file.

        Validate file magic bytes (http.DetectContentType).

        Generate a unique key: fmt.Sprintf("avatars/%s/%s.webp", userID, uuid.NewString()).

        Upload byte stream to S3/R2 bucket with Content-Type: image/webp and public-read access (or served via CDN domain).

        Query current avatar_storage_key from DB; if present and not a Google CDN URL, issue a background delete to S3/R2.

        Update users table with new avatar_url (CDN URL) and avatar_storage_key.

        Return 200 OK with { "avatar_url": "https://cdn.example.com/avatars/..." }.

5. ANDROID CLIENT SPECIFICATION (Jetpack Compose + Kotlin)
1. Photo Picker Integration  
val pickMedia = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
) { uri: Uri? ->
    uri?.let { viewModel.onAvatarSelected(context, it) }
}

// In Composable:
Box(
    modifier = Modifier
        .size(100.dp)
        .clip(CircleShape)
        .clickable {
            pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
) {
    AsyncImage(
        model = uiState.avatarUrl,
        contentDescription = "Profile Avatar",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
    // Overlay edit icon badge
}

val pickMedia = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
) { uri: Uri? ->
    uri?.let { viewModel.onAvatarSelected(context, it) }
}

// In Composable:
Box(
    modifier = Modifier
        .size(100.dp)
        .clip(CircleShape)
        .clickable {
            pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
) {
    AsyncImage(
        model = uiState.avatarUrl,
        contentDescription = "Profile Avatar",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
    // Overlay edit icon badge
}
2. Client-Side Image Processing & Upload

    Read the image Uri using ContentResolver.

    Decode and downsample the bitmap to a max resolution of 512x512 px.

    Compress into a WebP/JPEG byte array (quality 85%).

    Transmit using OkHttp / Retrofit with MultipartBody.Part.createFormData("avatar", "avatar.webp", requestBody).

    Update the UI state with optimistic preview, and confirm with server response.
---

For a step-by-step walkthrough on configuring privacy-friendly image selection in modern Android apps, check out this [Jetpack Compose Photo Picker Tutorial](https://www.youtube.com/watch?v=i1AOR73LNXI).

This video is relevant because it demonstrates implementing Android's native `PickVisualMedia` contract and displaying selected media with Coil in Jetpack Compose without requiring broad storage permissions.
http://googleusercontent.com/youtube_content/1 *YouTube video views will be stored in your YouTube History, and your data will be stored and used by YouTube according to its [Terms of Service](https://www.youtube.com/static?template=terms)*