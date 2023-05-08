package com.smarteye.ui.pay

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.smarteye.QrCodeDrawable
import com.smarteye.QrCodeViewModel
import com.smarteye.databinding.FragmentPayBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PayFragment : Fragment() {

    private var _binding: FragmentPayBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
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
        val payViewModel =
                ViewModelProvider(this).get(PayViewModel::class.java)

        _binding = FragmentPayBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textPay
        payViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // Request camera permissions was done in activity
        startCamera()

        cameraExecutor = Executors.newSingleThreadExecutor()
        println("PayFragment::onCreateView")
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        cameraExecutor.shutdown()
        barcodeScanner.close()
        println("PayFragment::onDestroyView")
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun startCamera() {
        var cameraController = LifecycleCameraController(attachedContext.applicationContext)
        val previewView: PreviewView = binding.previewPay

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(attachedContext.applicationContext),
            MlKitAnalyzer(
                listOf(barcodeScanner),
                CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                ContextCompat.getMainExecutor(attachedContext.applicationContext)
            ) { result: MlKitAnalyzer.Result? ->
                val barcodeResults = result?.getValue(barcodeScanner)
                if ((barcodeResults == null) ||
                    (barcodeResults.size == 0) ||
                    (barcodeResults.first() == null)
                ) {
                    previewView.overlay.clear()
                    previewView.setOnTouchListener { _, _ -> false } //no-op
                    return@MlKitAnalyzer
                }

                val qrCodeViewModel = QrCodeViewModel(barcodeResults[0])
                val qrCodeDrawable = QrCodeDrawable(qrCodeViewModel)

                previewView.setOnTouchListener(qrCodeViewModel.qrCodeTouchCallback)
                previewView.overlay.clear()
                previewView.overlay.add(qrCodeDrawable)
            }
        )

        cameraController.bindToLifecycle(viewLifecycleOwner)
        previewView.controller = cameraController
    }
}