package com.stockmaster.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * 拍照动作工厂：返回「临时文件 -> 发起拍照」的启动函数，供各录入页复用。
 *
 * 内部处理 CAMERA 运行时权限：manifest 声明了 CAMERA 时，未授权直接调 TakePicture 会抛
 * SecurityException（表现为点击卡死且不弹权限框），必须先走运行时权限申请；
 * 授权后自动续接拍照，拒绝则提示用户去系统设置开启。
 *
 * @param launchCapture 拿到 FileProvider Uri 后真正发起拍照的动作，通常为 takePictureLauncher.launch(uri)
 */
@Composable
fun rememberTakePhotoAction(
    launchCapture: (Uri) -> Unit
): (File) -> Unit {
    val context = LocalContext.current
    val currentLaunch by rememberUpdatedState(launchCapture)
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val uri = pendingUri
        pendingUri = null
        if (granted && uri != null) {
            currentLaunch(uri)
        } else if (!granted) {
            Toast.makeText(context, "未获得相机权限，无法拍照；可在系统设置中手动开启", Toast.LENGTH_LONG).show()
        }
    }

    return { tempPhoto ->
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempPhoto
        )
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            currentLaunch(uri)
        } else {
            pendingUri = uri
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}
