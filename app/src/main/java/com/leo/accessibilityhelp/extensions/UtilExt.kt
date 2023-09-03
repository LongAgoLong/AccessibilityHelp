package com.leo.accessibilityhelp.extensions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.AssetManager
import com.leo.commonutil.app.AppInfoUtil
import com.leo.commonutil.storage.IOUtil
import com.leo.system.context.ContextHelper
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


fun IOUtil.readAssetsString(context: Context, assetsFileName: String): String {
    val resultBuilder = StringBuilder()
    val assetManager: AssetManager = context.assets
    var bufferedReader: BufferedReader? = null
    try {
        bufferedReader =
            BufferedReader(InputStreamReader(assetManager.open(assetsFileName), "UTF-8"))
        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            resultBuilder.append(line)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        try {
            bufferedReader?.run { close() }
        } catch (e: IOException) {
        }
    }
    return resultBuilder.toString()
}

fun IOUtil.readAssetsStringList(context: Context, assetsFileName: String): List<String> {
    val resultList = mutableListOf<String>()
    val assetManager: AssetManager = context.assets
    var bufferedReader: BufferedReader? = null
    try {
        bufferedReader =
            BufferedReader(InputStreamReader(assetManager.open(assetsFileName), "UTF-8"))
        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            line?.let { resultList.add(it) }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        try {
            bufferedReader?.run { close() }
        } catch (e: IOException) {
        }
    }
    return resultList
}

/**
 * 获取正在运行桌面包名（注：存在多个桌面时且未指定默认桌面时，该方法返回Null,使用时需处理这个情况）
 */
fun AppInfoUtil.getLauncherPackage(context: Context = ContextHelper.context): String? {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_HOME)
    val res = context.packageManager.resolveActivity(intent, 0)
    if (res!!.activityInfo == null) {
        // should not happen. A home is always installed, isn't it?
        return null
    }
    return if (res.activityInfo.packageName == "android") {
        // 有多个桌面程序存在，且未指定默认项时；
        null
    } else {
        res.activityInfo.packageName
    }
}

fun AppInfoUtil.getLaunchActivityByPackage(
    context: Context = ContextHelper.context,
    packageName: String
): String {
    val pm: PackageManager = context.packageManager
    val intentToResolve = Intent(Intent.ACTION_MAIN)
    intentToResolve.addCategory(Intent.CATEGORY_INFO)
    intentToResolve.setPackage(packageName)
    var ris = pm.queryIntentActivities(intentToResolve, 0)
    if (ris.isEmpty()) {
        // reuse the intent instance
        intentToResolve.removeCategory(Intent.CATEGORY_INFO)
        intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER)
        intentToResolve.setPackage(packageName)
        ris = pm.queryIntentActivities(intentToResolve, 0)
    }
    return if (ris.isEmpty()) "" else ris[0].activityInfo.name
}

fun AppInfoUtil.getPhoneAllActivities(context: Context = ContextHelper.context): Map<String, List<String>> {
    val map = mutableMapOf<String, List<String>>()
    val packageManager: PackageManager = context.packageManager
    val queryIntentActivities = mutableListOf<ResolveInfo>()
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        queryIntentActivities.addAll(
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        )
    } else {
        queryIntentActivities.addAll(packageManager.queryIntentActivities(intent, 0))
    }

    queryIntentActivities.forEach {
        val packageName = it.activityInfo.packageName
        map[packageName] = getAllActivitiesByPackage(packageName = packageName)
    }
    // 获取launcher
    getLauncherPackage()?.let {
        map[it] = getAllActivitiesByPackage(packageName = it)
    }
    return map
}

fun AppInfoUtil.getAllActivitiesByPackage(
    context: Context = ContextHelper.context,
    packageName: String
): MutableList<String> {
    val activities = mutableListOf<String>()
    try {
        val packageManager: PackageManager = context.packageManager
        val packageInfo: PackageInfo = packageManager.getPackageInfo(
            packageName, PackageManager.GET_ACTIVITIES
        )
        packageInfo.activities.forEach { activityInfo ->
            activities.add(activityInfo.name)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return activities
}