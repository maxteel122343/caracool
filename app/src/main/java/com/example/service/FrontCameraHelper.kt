package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FrontCameraHelper {
    private const val TAG = "FrontCameraHelper"

    /**
     * Attempts to silently capture a selfie with the front camera using Camera2 API.
     * If camera hardware / permission is unavailable, it gracefully generates a personalized
     * unlock photo so the flow always succeeds.
     */
    suspend fun captureFrontSelfie(
        context: Context,
        userName: String,
        unlockCount: Int
    ): String = withContext(Dispatchers.IO) {
        val hasCameraPerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPerm) {
            Log.d(TAG, "Camera permission not granted, generating selfie card.")
            return@withContext generateFallbackSelfieFile(context, userName, unlockCount)
        }

        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return@withContext generateFallbackSelfieFile(context, userName, unlockCount)

            var frontCameraId: String? = null
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = id
                    break
                }
            }

            if (frontCameraId == null && cameraManager.cameraIdList.isNotEmpty()) {
                frontCameraId = cameraManager.cameraIdList[0]
            }

            if (frontCameraId == null) {
                return@withContext generateFallbackSelfieFile(context, userName, unlockCount)
            }

            val capturedPath = capturePhotoFromCamera2(context, cameraManager, frontCameraId)
            if (!capturedPath.isNullOrBlank()) {
                return@withContext capturedPath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in captureFrontSelfie Camera2: ${e.message}", e)
        }

        return@withContext generateFallbackSelfieFile(context, userName, unlockCount)
    }

    @SuppressLint("MissingPermission")
    private suspend fun capturePhotoFromCamera2(
        context: Context,
        cameraManager: CameraManager,
        cameraId: String
    ): String? = withContext(Dispatchers.IO) {
        var handlerThread: HandlerThread? = null
        var cameraDevice: CameraDevice? = null
        var imageReader: ImageReader? = null
        var capturedBitmap: Bitmap? = null

        try {
            handlerThread = HandlerThread("CameraBackground").apply { start() }
            val backgroundHandler = Handler(handlerThread.looper)

            val width = 720
            val height = 1280
            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2)

            val lock = Object()
            var isCaptured = false

            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)

                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        capturedBitmap = rotateBitmapIfNecessary(bitmap, 270f)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding captured camera frame: ${e.message}")
                } finally {
                    image.close()
                    synchronized(lock) {
                        isCaptured = true
                        lock.notifyAll()
                    }
                }
            }, backgroundHandler)

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    try {
                        val surface = imageReader.surface
                        @Suppress("DEPRECATION")
                        camera.createCaptureSession(
                            listOf(surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    try {
                                        val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                            addTarget(surface)
                                            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                            set(CaptureRequest.JPEG_ORIENTATION, 270)
                                        }
                                        session.capture(captureBuilder.build(), null, backgroundHandler)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error capturing still request: ${e.message}")
                                        synchronized(lock) {
                                            isCaptured = true
                                            lock.notifyAll()
                                        }
                                    }
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    synchronized(lock) {
                                        isCaptured = true
                                        lock.notifyAll()
                                    }
                                }
                            },
                            backgroundHandler
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed creating camera capture session: ${e.message}")
                        synchronized(lock) {
                            isCaptured = true
                            lock.notifyAll()
                        }
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                    synchronized(lock) {
                        isCaptured = true
                        lock.notifyAll()
                    }
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    synchronized(lock) {
                        isCaptured = true
                        lock.notifyAll()
                    }
                }
            }, backgroundHandler)

            // Wait up to 3.5 seconds for capture
            synchronized(lock) {
                if (!isCaptured) {
                    lock.wait(3500)
                }
            }

            // Process face detection and Paçoca frame with coroutines
            val rawBmp = capturedBitmap
            if (rawBmp != null) {
                val db = com.example.data.database.AppDatabase.getDatabase(context)
                val settings = db.settingsDao().getSettingsDirect()
                val isKool = settings?.isCaraDeKoolMode ?: false
                val isFrameEnabled = settings?.isPhotoFrameEnabled ?: true
                return@withContext PacocaFrameHelper.processAndApplyPacocaFrame(
                    context = context,
                    sourceBitmap = rawBmp,
                    isKoolMode = isKool,
                    isFrameEnabled = isFrameEnabled
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during Camera2 capture flow: ${e.message}")
        } finally {
            try {
                cameraDevice?.close()
                imageReader?.close()
                handlerThread?.quitSafely()
            } catch (_: Exception) {}
        }

        return@withContext null
    }

    private fun rotateBitmapIfNecessary(source: Bitmap, angle: Float): Bitmap {
        return try {
            val matrix = Matrix().apply { postRotate(angle) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } catch (_: Exception) {
            source
        }
    }

    private fun generateFallbackSelfieFile(context: Context, userName: String, unlockCount: Int): String {
        val bitmap = WallpaperHelper.createDefaultPacocaWallpaper(
            context = context,
            title = if (userName.isNotBlank()) userName else "Cara de Paçoca",
            emoji = "🥜",
            unlockCount = unlockCount
        )
        val file = File(context.filesDir, "selfie_card_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file.absolutePath
    }
}
