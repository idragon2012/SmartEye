/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smarteye

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * A ViewModel for encapsulating the data for a QR Code, including the encoded data, the bounding
 * box, and the touch behavior on the QR Code.
 *
 * As is, this class only handles displaying the QR Code data if it's a URL. Other data types
 * can be handled by adding more cases of Barcode.TYPE_URL in the init block.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class QrCodeViewModel(barcode: Barcode) {
    var boundingRect: Rect = barcode.boundingBox!!
    var qrContent: String = ""
    var qrCodeTouchCallback = { v: View, e: MotionEvent -> false} //no-op

    private fun invokeAlipay(context: Context) {
        val uri:Uri = Uri.parse("alipayqr://platformapi/startapp?saId=10000007")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

    private fun invokeWechatPay(context: Context) {
        val pkgName = "com.tencent.mm"
        val intent  = Intent()
        intent.component = ComponentName(pkgName, "com.tencent.mm.ui.LauncherUI")
        intent.putExtra("LauncherUI.From.Scaner.Shortcut", true)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.action = "android.intent.action.VIEW"
        context.startActivity(intent)
    }
    init {

        println("type=" + barcode.valueType.toString())
        when (barcode.valueType) {
            Barcode.TYPE_URL -> {
                qrContent = barcode.url!!.url!!
                qrCodeTouchCallback = { v: View, e: MotionEvent ->
                    if (e.action == MotionEvent.ACTION_DOWN && boundingRect.contains(
                            e.getX().toInt(), e.getY().toInt()
                        )
                    ) {
                        println(qrContent)
                        if (qrContent.contains("https://qr.alipay.com/")) {
                            invokeAlipay(v.context)
                        } else {
                            val openBrowserIntent = Intent(Intent.ACTION_VIEW)
                            openBrowserIntent.data = Uri.parse(qrContent)
                            v.context.startActivity(openBrowserIntent)
                        }
                    }
                    true // return true from the callback to signify the event was handled
                }
            }

            Barcode.TYPE_TEXT -> {
                qrContent = barcode.rawValue.toString()
                qrCodeTouchCallback = { v: View, e: MotionEvent -> Boolean
                    if (e.action == MotionEvent.ACTION_DOWN && boundingRect.contains(
                            e.getX().toInt(), e.getY().toInt()
                        )
                    ) {
                        println(qrContent)
                        if (qrContent.contains("wxp://")) {
                            invokeWechatPay(v.context)
                        } else {
                            qrContent = "Unsupported data type: ${qrContent}"
                        }
                    }
                    true
                }
            }
            // Add other QR Code types here to handle other types of data,
            // like Wifi credentials.
            else -> {
                qrContent = "Unsupported data type: ${barcode.rawValue.toString()}"
            }
        }
    }
}