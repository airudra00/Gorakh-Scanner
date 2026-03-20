package com.gorakhscanner.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

class MainActivity : AppCompatActivity() {

    private lateinit var resultTextView: TextView
    private lateinit var docImageView: ImageView
    private lateinit var shareButton: Button
    private var pdfUriToShare: Uri? = null

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            
            scanResult?.pdf?.let { pdf ->
                pdfUriToShare = pdf.uri
                val pageCount = pdf.pageCount
                resultTextView.text = "PDF generated successfully!\nPages: $pageCount"
                shareButton.visibility = View.VISIBLE
            } ?: run {
                resultTextView.text = "Error: No PDF generated."
            }

            // Display the first scanned page as a preview
            scanResult?.pages?.firstOrNull()?.let { page ->
                val imageUri = page.imageUri
                docImageView.setImageURI(imageUri)
            }
        } else {
            Toast.makeText(this, "Scan cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val scanButton: Button = findViewById(R.id.btn_scan)
        shareButton = findViewById(R.id.btn_share)
        resultTextView = findViewById(R.id.tv_result)
        docImageView = findViewById(R.id.iv_document)

        // Configure ML Kit Document Scanner
        // This acts exactly like CamScanner (Auto-capture, smart crop, filters)
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(25) // Up to 25 pages in one PDF
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)
        
        scanButton.setOnClickListener {
            scanner.getStartScanIntent(this)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to start scanner: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        shareButton.setOnClickListener {
            pdfUriToShare?.let { uri ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share or Save Gorakh Scanner PDF"))
            }
        }
    }
}
