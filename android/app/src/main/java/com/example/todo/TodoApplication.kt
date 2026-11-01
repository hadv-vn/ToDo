package com.example.todo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class gốc — bắt buộc để Hilt sinh component DI toàn app.
 * Khai báo trong AndroidManifest.xml qua android:name=".TodoApplication".
 */
@HiltAndroidApp
class TodoApplication : Application()
