package com.smarteye.ui.identify

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.smarteye.R
import com.smarteye.databinding.FragmentIdentifyBinding
import com.smarteye.invoke.ChatGPTUtil
import com.smarteye.invoke.ImageDetectUtil
import com.smarteye.invoke.InfoUtil
import com.smarteye.tflite.ObjectDetectionHelper
import org.json.JSONArray
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.min
import kotlin.math.max

class IdentifyFragment : Fragment() {

    private var _binding: FragmentIdentifyBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var bitmapBuffer: Bitmap
    private var latest_location = RectF(0f, 0f, 3f, 3f)

    private val executor = Executors.newSingleThreadExecutor()

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private val isFrontFacing get() = lensFacing == CameraSelector.LENS_FACING_FRONT

    private var pauseAnalysis = false
    private var imageRotationDegrees: Int = 0
    private val tfImageBuffer = TensorImage(DataType.UINT8)

    private lateinit var attachedContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        attachedContext = context
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIdentifyBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _binding!!.cameraCaptureButton.setOnClickListener {
/*
            // Disable all camera controls
            it.isEnabled = false

            if (pauseAnalysis) {
                // If image analysis is in paused state, resume it
                pauseAnalysis = false
                _binding!!.imagePredicted.visibility = View.GONE

            } else {
                // Otherwise, pause image analysis and freeze image
                pauseAnalysis = true
                val matrix = Matrix().apply {
                    postRotate(imageRotationDegrees.toFloat())
                    if (isFrontFacing) postScale(-1f, 1f)
                }

                //println("buffer.width: ${bitmapBuffer.width}, buffer.height: ${bitmapBuffer.height}")
                val rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)
                //println("rotatedBitmap.width: ${rotatedBitmap.width}, rotatedBitmap.height: ${rotatedBitmap.height}")

               // println(latest_location)
                val location = RectF(max(0f, latest_location.left)*rotatedBitmap.width,
                    max(0f, latest_location.top) * rotatedBitmap.height,
                    max(0f, latest_location.right) * rotatedBitmap.width,
                    max(0f, latest_location.bottom) * rotatedBitmap.height)
                //println(location)
                //println("x: ${location.left}, y: ${location.top}, width: ${location.right-location.left}, height: ${location.bottom-location.top}")
                val x = location.left.toInt()
                val y = location.top.toInt()
                val width = min(bitmapBuffer.width-location.left.toInt(), location.right.toInt()-location.left.toInt())
                val height = min(bitmapBuffer.height-location.top.toInt(), location.bottom.toInt()-location.top.toInt())
                //println("x: $x, y: $y, width: $width, height: $height")
                val croppedImage = Bitmap.createBitmap(rotatedBitmap, x, y, min(rotatedBitmap.width-x, width), min(rotatedBitmap.height-y, height))
                //println("croppedImage.width: ${croppedImage.width}, croppedImage.height: ${croppedImage.height}")
                _binding!!.imagePredicted.scaleType = ImageView.ScaleType.FIT_CENTER
                _binding!!.imagePredicted.setImageBitmap(croppedImage)
                _binding!!.imagePredicted.visibility = View.VISIBLE
                binding.boxPrediction.visibility = View.GONE
                //binding.textPrediction.visibility = View.GONE
            }

            // Re-enable camera controls
            it.isEnabled = true*/

            val matrix = Matrix().apply {
                postRotate(imageRotationDegrees.toFloat())
                if (isFrontFacing) postScale(-1f, 1f)
            }
            val rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)

            thread {
                val message = ImageDetectUtil.advancedGeneral(rotatedBitmap);//调用百度接口进行图像识别

                Looper.prepare();

                val alertDialog = AlertDialog.Builder(context).apply {
                    setTitle("识别结果")
                    setMessage(InfoUtil.transInfo(message))//组合将要展示的信息，并查询 ChatGPT
                    setPositiveButton("确定", null) // 添加确定按钮，并设置点击事件监听器
                }.create()

                alertDialog.setOnShowListener {
                    val window = alertDialog.window
                    window?.let {
                        val layoutParams = WindowManager.LayoutParams().apply {
                            copyFrom(it.attributes)
                            alpha = 0.6f // 设置透明度，0-1 之间的浮点数
                        }
                        it.attributes = layoutParams
                    }
                }

                alertDialog.show() // 显示弹出框
                Looper.loop();
            }
        }

        return root
    }

    override fun onDestroyView() {
        // Terminate all outstanding analyzing jobs (if there is any).
        executor.apply {
            shutdown()
            awaitTermination(1000, TimeUnit.MILLISECONDS)
        }

        // Release TFLite resources.
        tflite.close()
        nnApiDelegate.close()

        super.onDestroyView()
        _binding = null
    }

    private val tfImageProcessor by lazy {
        val cropSize = minOf(bitmapBuffer.width, bitmapBuffer.height)
        ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(
                ResizeOp(
                    tfInputSize.height, tfInputSize.width, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR)
            )
            .add(Rot90Op(-imageRotationDegrees / 90))
            .add(NormalizeOp(0f, 1f))
            .build()
    }

    private val nnApiDelegate by lazy  {
        NnApiDelegate()
    }

    private val tflite by lazy {
        Interpreter(
            FileUtil.loadMappedFile(attachedContext, MODEL_PATH),
            Interpreter.Options().addDelegate(nnApiDelegate))
    }
    private val detector by lazy {
        ObjectDetectionHelper(
            tflite,
            FileUtil.loadLabels(attachedContext, LABELS_PATH)
        )
    }

    private val tfInputSize by lazy {
        val inputIndex = 0
        val inputShape = tflite.getInputTensor(inputIndex).shape()
        Size(inputShape[2], inputShape[1]) // Order of axis is: {1, height, width, 3}
    }

    /** Declare and bind preview and analysis use cases */
    @SuppressLint("UnsafeExperimentalUsageError")
    private fun bindCameraUseCases() = binding.identifyPreview.post {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(attachedContext)
        cameraProviderFuture.addListener ({

            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            // Set up the view finder use case to display camera preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(binding.identifyPreview.display.rotation)
                .build()

            // Set up the image analysis use case which will process frames in real time
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(binding.identifyPreview.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            var frameCounter = 0
            var lastFpsTimestamp = System.currentTimeMillis()

            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
                if (!::bitmapBuffer.isInitialized) {
                    // The image rotation and RGB image buffer are initialized only once
                    // the analyzer has started running
                    imageRotationDegrees = image.imageInfo.rotationDegrees
                    bitmapBuffer = Bitmap.createBitmap(
                        image.width, image.height, Bitmap.Config.ARGB_8888)
                    println("image.width: ${image.width}, image.height: ${image.height}")
                    println("bitmapBuffer.width: ${bitmapBuffer.width}, bitmapBuffer.height: ${bitmapBuffer.height}")
                    println("identifyPreview.width: ${binding.identifyPreview.width}, identifyPreview.height: ${binding.identifyPreview.height}")
                    println("identifyPredicted.width: ${binding.imagePredicted.width}, identifyPredicted.height: ${binding.imagePredicted.height}")
                }

                // Early exit: image analysis is in paused state
                if (pauseAnalysis) {
                    image.close()
                    return@Analyzer
                }

                // Copy out RGB bits to our shared buffer
                image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer)  }

                /*                // Process the image in Tensorflow
                                val tfImage =  tfImageProcessor.process(tfImageBuffer.apply { load(bitmapBuffer) })

                                // Perform the object detection for the current frame
                                val predictions = detector.predict(tfImage)

                                // Report only the top prediction
                                reportPrediction(predictions.maxByOrNull { it.score })

                                // Compute the FPS of the entire pipeline
                                val frameCount = 10
                                if ((++frameCounter % frameCount) == 0) {
                                    frameCounter = 0
                                    val now = System.currentTimeMillis()
                                    val delta = now - lastFpsTimestamp
                                    val fps = 1000 * frameCount.toFloat() / delta
                                    Log.d(TAG, "FPS: ${"%.02f".format(fps)} with tensorSize: ${tfImage.width} x ${tfImage.height}")
                                    lastFpsTimestamp = now
                                }*/
            })

            // Create a new camera selector each time, enforcing lens facing
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            // Apply declared configs to CameraX using the same lifecycle owner
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this as LifecycleOwner, cameraSelector, preview, imageAnalysis)

            // Use the camera object to link our preview use case with the view
            preview.setSurfaceProvider(binding.identifyPreview.surfaceProvider)

        }, ContextCompat.getMainExecutor(attachedContext))
    }

    @SuppressLint("SetTextI18n")
    private fun reportPrediction(
        prediction: ObjectDetectionHelper.ObjectPrediction?
    ) = binding.identifyPreview.post {

        // Early exit: if prediction is not good enough, don't report it
        if (prediction == null || prediction.score < ACCURACY_THRESHOLD) {
            Log.d(IdentifyFragment::class.java.simpleName, "Low confidence prediction: $prediction")
            binding.boxPrediction.visibility = View.GONE
            binding.textPrediction.visibility = View.GONE
            return@post
        }

        // Location has to be mapped to our local coordinates
        val location = mapOutputCoordinates(prediction.location)

        // Update the text and UI
        binding.textPrediction.text = "${"%.2f".format(prediction.score)} ${prediction.label}"
        Log.d(IdentifyFragment::class.java.simpleName, binding.textPrediction.text.toString())
        (binding.boxPrediction.layoutParams as ViewGroup.MarginLayoutParams).apply {
            topMargin = location.top.toInt()
            leftMargin = location.left.toInt()
            width = min(binding.identifyPreview.width, location.right.toInt() - location.left.toInt())
            height = min(binding.identifyPreview.height, location.bottom.toInt() - location.top.toInt())
        }

        latest_location = prediction.location

        // Make sure all UI elements are visible
        binding.boxPrediction.visibility = View.VISIBLE
        binding.textPrediction.visibility = View.VISIBLE
    }

    /**
     * Helper function used to map the coordinates for objects coming out of
     * the model into the coordinates that the user sees on the screen.
     */
    private fun mapOutputCoordinates(location: RectF): RectF {

        // Step 1: map location to the preview coordinates
        val previewLocation = RectF(
            location.left * binding.identifyPreview.width,
            location.top * binding.identifyPreview.height,
            location.right * binding.identifyPreview.width,
            location.bottom * binding.identifyPreview.height
        )
        //println("previewLocation: $previewLocation")
        // Step 2: compensate for camera sensor orientation and mirroring
        val isFrontFacing = lensFacing == CameraSelector.LENS_FACING_FRONT
        val correctedLocation = if (isFrontFacing) {
            RectF(
                binding.identifyPreview.width - previewLocation.right,
                previewLocation.top,
                binding.identifyPreview.width - previewLocation.left,
                previewLocation.bottom)
        } else {
            previewLocation
        }
        //println("previewLocation: $previewLocation")
        // Step 3: compensate for 1:1 to 4:3 aspect ratio conversion + small margin
        val margin = 0.1f
        val requestedRatio = 4f / 3f
        val midX = (correctedLocation.left + correctedLocation.right) / 2f
        val midY = (correctedLocation.top + correctedLocation.bottom) / 2f
        return if (binding.identifyPreview.width < binding.identifyPreview.height) {
            RectF(
                midX - (1f + margin) * requestedRatio * correctedLocation.width() / 2f,
                midY - (1f - margin) * correctedLocation.height() / 2f,
                midX + (1f + margin) * requestedRatio * correctedLocation.width() / 2f,
                midY + (1f - margin) * correctedLocation.height() / 2f
            )
        } else {
            RectF(
                midX - (1f - margin) * correctedLocation.width() / 2f,
                midY - (1f + margin) * requestedRatio * correctedLocation.height() / 2f,
                midX + (1f - margin) * correctedLocation.width() / 2f,
                midY + (1f + margin) * requestedRatio * correctedLocation.height() / 2f
            )
        }
    }

    override fun onResume() {
        super.onResume()
        bindCameraUseCases()
    }

    companion object {
        private val TAG = IdentifyFragment::class.java.simpleName

        private const val ACCURACY_THRESHOLD = 0.5f
        private const val MODEL_PATH = "coco_ssd_mobilenet_v1_1.0_quant.tflite"
        private const val LABELS_PATH = "coco_ssd_mobilenet_v1_1.0_labels.txt"
    }

}