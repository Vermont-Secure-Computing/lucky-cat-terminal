package com.vermont.possin

import android.content.Context
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.pdf.PrintedPdfDocument
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import java.io.FileOutputStream
import java.io.IOException

class ReceiptPrintAdapter(private val context: Context, private val receiptContent: String) : PrintDocumentAdapter() {

    private var pageHeight: Int = 0
    private var pageWidth: Int = 0
    private lateinit var pdfDocument: PrintedPdfDocument
    private lateinit var view: View

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: android.os.CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        pdfDocument = PrintedPdfDocument(context, newAttributes)

        pageHeight = newAttributes.mediaSize!!.heightMils / 1000 * 72
        pageWidth = newAttributes.mediaSize!!.widthMils / 1000 * 72

        if (cancellationSignal!!.isCanceled) {
            callback!!.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder("receipt.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()
        callback!!.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pageRanges: Array<out android.print.PageRange>,
        destination: android.os.ParcelFileDescriptor,
        cancellationSignal: android.os.CancellationSignal,
        callback: WriteResultCallback
    ) {
        val page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())

        if (cancellationSignal.isCanceled) {
            callback.onWriteCancelled()
            pdfDocument.close()
            return
        }

        drawPage(page)
        pdfDocument.finishPage(page)

        try {
            pdfDocument.writeTo(FileOutputStream(destination.fileDescriptor))
        } catch (e: IOException) {
            callback.onWriteFailed(e.toString())
            return
        } finally {
            pdfDocument.close()
        }

        callback.onWriteFinished(pageRanges)
    }

    private fun drawPage(page: PdfDocument.Page) {
        val canvas: Canvas = page.canvas

        // Inflate the view and set the content
        view = LayoutInflater.from(context).inflate(R.layout.receipt_layout, null)
        view.findViewById<TextView>(R.id.receiptContent).text = receiptContent

        // Measure and layout the view
        val measureWidth = View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY)
        val measureHeight = View.MeasureSpec.makeMeasureSpec(pageHeight, View.MeasureSpec.EXACTLY)
        view.measure(measureWidth, measureHeight)
        view.layout(0, 0, pageWidth, pageHeight)

        // Draw the view on the canvas
        view.draw(canvas)
    }
}