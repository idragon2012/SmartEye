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
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.smarteye.databinding.FragmentIdentifyBinding
import com.smarteye.invoke.ImageDetectUtil
import com.smarteye.invoke.InfoUtil
import com.smarteye.tflite.ObjectDetectionHelper
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
            // Disable all camera controls
            it.isEnabled = false

            if (pauseAnalysis) {
                // If image analysis is in paused state, resume it
                pauseAnalysis = false
                _binding!!.imagePredicted.visibility = View.GONE

            } else {
                /*
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
                */

                binding.cameraCaptureButton.isClickable = false

                val matrix = Matrix().apply {
                    postRotate(imageRotationDegrees.toFloat())
                    if (isFrontFacing) postScale(-1f, 1f)
                }
                val rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)

                thread {
                    // Initialize fake json message in case of empty access token for Baike
                    var message =
                        "{\"result_num\":5,\"result\":[{\"keyword\":\"笔记本电脑\",\"score\":0.926193,\"root\":\"商品-电脑办公\",\"baike_info\":{\"baike_url\":\"http://baike.baidu.com/item/%E7%AC%94%E8%AE%B0%E6%9C%AC%E7%94%B5%E8%84%91/213561\",\"image_url\":\"https://bkimg.cdn.bcebos.com/pic/bd3eb13533fa828ba61ed232e04b5634970a314eec9c\",\"description\":\"笔记本电脑(Laptop)，简称笔记本，又称“便携式电脑，手提电脑、掌上电脑或膝上型电脑”，特点是机身小巧。比台式机携带方便，是一种小型、便于携带的个人电脑。通常重1-3千克。当前发展趋势是体积越来越小，重量越来越轻，功能越来越强。为了缩小体积，笔记本电脑采用液晶显示器(液晶LCD屏)。除键盘外，还装有触摸板(Touchpad)或触控点(Pointing stick)作为定位设备(Pointing device)。笔记本电脑和台式机的区别在于便携性，它对主板、中央处理器、内存、显卡、电脑硬盘的容量等有不同要求。当今的笔记本电脑正在根据用途分化出不同的趋势，上网本趋于日常办公以及电影；商务本趋于稳定低功耗获得更长久的续航时间；家用本拥有不错的性能和很高的性价比，游戏本则是专门为了迎合少数人群外出游戏使用的；发烧级配置，娱乐体验效果好，当然价格不低，电池续航时间也不理想。全球市场上有很多品牌的笔记本电脑。依次为(不按顺序排列)：苹果(Apple)、联想(Lenovo)、惠普(HP)、华硕、宏碁(Acer)等。注：笔记本电脑的品牌分三线，一线、准一线、二线和三线。\"}},{\"keyword\":\"笔记本\",\"score\":0.617072,\"root\":\"商品-电脑办公\",\"baike_info\":{}},{\"keyword\":\"室内一角\",\"score\":0.380852,\"root\":\"建筑-室内\",\"baike_info\":{}},{\"keyword\":\"台式电脑\",\"score\":0.19285,\"root\":\"商品-数码产品\",\"baike_info\":{\"baike_url\":\"http://baike.baidu.com/item/%E5%8F%B0%E5%BC%8F%E7%94%B5%E8%84%91/1958355\",\"image_url\":\"https://bkimg.cdn.bcebos.com/pic/9825bc315c6034a8c0b07935c113495409237630\",\"description\":\"台式机，是一种独立相分离的计算机，完完全全跟其它部件无联系，相对于笔记本和上网本体积较大，主机、显示器等设备一般都是相对独立的，一般需要放置在电脑桌或者专门的工作台上。因此命名为台式机。台式电脑的优点就是耐用，以及价格实惠，和笔记本相比，相同价格前提下配置较好，散热性较好，配件若损坏更换价格相对便宜，缺点就是：笨重，耗电量大。电脑(Computer)是一种利用电子学原理根据一系列指令来对数据进行处理的机器。电脑可以分为两部分：软件系统,硬件系统。第一台电脑ENIAC于1946年2月14日宣告诞生。\"}},{\"keyword\":\"电脑\",\"score\":0.003849,\"root\":\"商品-电脑办公\",\"baike_info\":{}}],\"log_id\":1662722855597027057}"
                    var prefix = "ImageDetectUtil.accessToken is empty. Following message from Baike is fake:\n"
                    if (ImageDetectUtil.accessToken.isNotEmpty()) {
                        message = ImageDetectUtil.advancedGeneral(rotatedBitmap) //调用百度接口进行图像识别
                        prefix = ""
                    }

                    val objectMessage = prefix +
                        InfoUtil.transInfo(message) // Call chat-gpt3 to generate a sentence based on the image recognition result

                    Looper.prepare()

                    val alertDialog = AlertDialog.Builder(context).apply {
                        setTitle("识别结果")
                        setMessage(objectMessage)//组合将要展示的信息
                        setPositiveButton("确定", { dialog, which ->
                            binding.cameraCaptureButton.isClickable = true
                        }) // 添加确定按钮，并设置点击事件监听器
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
                    Looper.loop()
                }
            }
            // Re-enable camera controls
            it.isEnabled = true
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
                               // Process the image in Tensorflow
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
                                }
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