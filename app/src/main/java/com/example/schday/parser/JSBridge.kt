package com.example.schday.parser

import android.webkit.JavascriptInterface
import org.json.JSONArray

class JSBridge(private val onParsed: (List<ParsedCourse>) -> Unit) {

    @JavascriptInterface
    fun sendCourseData(jsonStr: String) {
        val list = mutableListOf<ParsedCourse>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.getString("name")
                val teacher = obj.optString("teacher", "")
                val classroom = obj.optString("classroom", "")
                val day = obj.getInt("day") // 1-7
                val start = obj.getInt("start") // 1-12
                val end = obj.getInt("end") // 1-12
                val weeks = obj.getString("weeks") // e.g. "1,2,3,4,5"
                list.add(ParsedCourse(name, teacher, classroom, day, start, end, weeks))
            }
            onParsed(list)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class ParsedCourse(
    val name: String,
    val teacher: String,
    val classroom: String,
    val day: Int,
    val start: Int,
    val end: Int,
    val weeks: String
)
