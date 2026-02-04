package com.colma.colmaApp.utils

import android.content.Context

object ResourceUtils {
    fun getResourceId(context: Context, resourceName: String, resourceType: String): Int {
        return context.resources.getIdentifier(resourceName, resourceType, context.packageName)
    }
}
