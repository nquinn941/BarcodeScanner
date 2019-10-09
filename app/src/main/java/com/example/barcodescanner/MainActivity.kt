package com.example.barcodescanner

import androidx.appcompat.app.AppCompatActivity

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.wonderkiln.camerakit.CameraKitError
import com.wonderkiln.camerakit.CameraKitEvent
import com.wonderkiln.camerakit.CameraKitEventListener
import com.wonderkiln.camerakit.CameraKitImage
import com.wonderkiln.camerakit.CameraKitVideo
import com.wonderkiln.camerakit.CameraView

import dmax.dialog.SpotsDialog

class MainActivity : AppCompatActivity() {

    internal lateinit var cameraView: CameraView
    internal lateinit var btnDetect: Button
    internal lateinit var waitingDialog: AlertDialog

    override fun onPause() {
        super.onPause()
        cameraView.stop()
    }

    override fun onResume() {
        super.onResume()
        cameraView.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        cameraView = findViewById<View>(R.id.cameraview) as CameraView
        btnDetect = findViewById<View>(R.id.btn_detect) as Button
        waitingDialog = SpotsDialog.Builder().setContext(this)
                .setMessage("Please Wait").setCancelable(false).build()


        btnDetect.setOnClickListener {
            cameraView.start()
            cameraView.captureImage()
        }

        cameraView.addCameraKitListener(object : CameraKitEventListener {
            override fun onEvent(cameraKitEvent: CameraKitEvent) {

            }

            override fun onError(cameraKitError: CameraKitError) {

            }

            override fun onImage(cameraKitImage: CameraKitImage) {
                waitingDialog.show()
                var bitmap = cameraKitImage.bitmap
                bitmap = Bitmap.createScaledBitmap(bitmap, cameraView.width, cameraView.height, false)
                cameraView.stop()

                runDetector(bitmap)
            }

            override fun onVideo(cameraKitVideo: CameraKitVideo) {

            }
        })
    }

    private fun runDetector(bitmap: Bitmap) {
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_EAN_13).build()

        val detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)

        detector.detectInImage(image)
                .addOnSuccessListener { firebaseVisionBarcodes -> processResult(firebaseVisionBarcodes) }
                .addOnFailureListener { e -> Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show() }
    }

    private fun processResult(firebaseVisionBarcodes: List<FirebaseVisionBarcode>) {
        for (item in firebaseVisionBarcodes) {
            val valueType = item.valueType

            when (valueType) {

                FirebaseVisionBarcode.TYPE_PRODUCT -> {
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage(item.rawValue)
                    builder.setPositiveButton("Ok") { dialogInterface, i -> dialogInterface.dismiss() }
                    val dialog = builder.create()
                    dialog.show()

                    val openURL = Intent(Intent.ACTION_VIEW)
                    openURL.data = Uri.parse("https://world.openfoodfacts.org/api/v0/product/${item.rawValue}.json")
                    startActivity(openURL)
                }

                else -> {

                }
            }
        }
        waitingDialog.dismiss()
    }
}
