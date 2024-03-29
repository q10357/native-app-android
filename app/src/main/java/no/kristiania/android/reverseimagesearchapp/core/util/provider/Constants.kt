package no.kristiania.android.reverseimagesearchapp.core.util.provider

object Constants {
    const val UPLOAD_URI = "http://api-edu.gtl.ai/api/v1/imagesearch/upload"
    const val BASE_URI = "http://api-edu.gtl.ai/api/v1/imagesearch/"
    const val DATABASE_NAME = "ImageDatabase"
    const val SPLASH_SCREEN_TIME = 10L
    //Not constants, but we place them here for simplicity
    val fetchUrls = arrayListOf<String>("bing", "google", "tineye")
}