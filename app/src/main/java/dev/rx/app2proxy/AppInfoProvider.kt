package dev.rx.app2proxy

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.util.Log

class AppInfoProvider : ContentProvider() {
    
    companion object {
        private const val TAG = "AppInfoProvider"
        private const val AUTHORITY = "dev.rx.app2proxy.appinfo"
        private const val CODE_APP_INFO = 1
        
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "info", CODE_APP_INFO)
        }
    }
    
    override fun onCreate(): Boolean {
        Log.d(TAG, "AppInfoProvider создан для предотвращения вылетов системных настроек")
        return true
    }
    
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        Log.d(TAG, "Запрос информации о приложении: $uri")
        
        return when (uriMatcher.match(uri)) {
            CODE_APP_INFO -> {
                val cursor = MatrixCursor(arrayOf("package_name", "version_name", "version_code", "label"))
                
                try {
                    val context = context ?: return cursor
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val appInfo = packageInfo.applicationInfo ?: return cursor
                    
                    val versionName = packageInfo.versionName ?: "1.0"
                    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toString()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toString()
                    }
                    val label = context.packageManager.getApplicationLabel(appInfo).toString()
                    
                    cursor.addRow(arrayOf(context.packageName, versionName, versionCode, label))
                    
                    Log.d(TAG, "Возвращена информация о приложении: $label v$versionName ($versionCode)")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка получения информации о приложении", e)
                    // Возвращаем пустой курсор вместо null для предотвращения вылетов
                }
                
                cursor
            }
            else -> {
                Log.w(TAG, "Неизвестный URI: $uri")
                null
            }
        }
    }
    
    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            CODE_APP_INFO -> "vnd.android.cursor.item/app_info"
            else -> null
        }
    }
    
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
