package com.eloi.retrolightgb.ui

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

actual class FilePickerLauncher(
    private val viewController: UIViewController,
    private val onResult: (ByteArray) -> Unit
) {
    private var delegate: DocumentPickerDelegate? = null

    actual fun launch() {
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeItem))
        picker.allowsMultipleSelection = false
        delegate = DocumentPickerDelegate(onResult)
        picker.delegate = delegate
        viewController.presentViewController(picker, animated = true, completion = null)
    }
}

private class DocumentPickerDelegate(
    private val onResult: (ByteArray) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        url.startAccessingSecurityScopedResource()
        try {
            val data = NSData.dataWithContentsOfURL(url) ?: return
            onResult(data.bytes!!.readBytes(data.length.toInt()))
        } finally {
            url.stopAccessingSecurityScopedResource()
        }
    }
}