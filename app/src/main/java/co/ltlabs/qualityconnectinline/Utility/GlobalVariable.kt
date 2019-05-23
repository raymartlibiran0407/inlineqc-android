package co.ltlabs.qualityconnectinline.Utility

class GlobalVariable {

    companion object {
        var accessToken = ""
        var currentLanguage = ""
        var defectStringList = ""
        var factory = "CM01"
        var hasBuiltInReader = false
        var hasInternet = true
        var isMaxCardReached = false
        var languageList = arrayOf<Array<String>>()
        var languageDescList = arrayOf<String>()
        var mfgLine = ""
        var mfgLineMultiple = "YTI-10" //YTI-01,YTI-10
        var postToExternal = true
        var product: String = "qc"
        val timerLoopSecond = 5000L
        val timerNoDelaySecond = 0L
        var translationMap = HashMap<String, String>()
        var urlExternal = "http://172.30.44.28"
        var urlInternal = "http://157.230.41.8" //"http://128.199.199.113", "http://157.230.41.8"
        var userId = ""
    }
}