package com.leo.accessibilityhelp.extensions

import android.content.Context
import android.content.res.AssetManager
import android.widget.CompoundButton
import com.leo.commonutil.storage.IOUtil
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