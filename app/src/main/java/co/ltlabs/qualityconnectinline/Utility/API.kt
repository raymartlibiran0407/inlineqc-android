package co.ltlabs.qualityconnectinline.Utility

import com.loopj.android.http.*

import android.content.Context
import android.preference.PreferenceManager
/*import cz.msebera.android.httpclient.HttpEntity
import cz.msebera.android.httpclient.entity.StringEntity*/
/*import cz.msebera.android.httpclient.HttpEntity*/
import org.apache.http.HttpEntity
/*import cz.msebera.android.httpclient.HttpEntity*/
/*import org.apache.http.HttpEntity*/
import java.security.KeyStore


class API {

    private val BASE_URL = ""
    /*var sampleURL: String = "http://172.16.27.157:3000/api/v1/users/"*/

    private val client = AsyncHttpClient()
    private val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())

    fun byPassSSL() {
        /*trustStore.load(null, null)
        val socketFactory = MySSLSocketFactory(trustStore)
        socketFactory.hostnameVerifier = MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER

        client.setTimeout(30000)
        client.setSSLSocketFactory(socketFactory)*/
        client.setTimeout(30000)
    }

    fun get(context: Context, url: String, params: RequestParams?, responseHandler: AsyncHttpResponseHandler) {
        if (GlobalVariable.hasInternet) {
            //responseHandler.useSynchronousMode = true
            byPassSSL()
            client.get(url, params, responseHandler)
            //client.get(getAbsoluteUrl(url, context), params, responseHandler)
            println("get")
        }
    }

    fun post(context: Context, url: String, params: RequestParams?, responseHandler: AsyncHttpResponseHandler) {
        if (GlobalVariable.hasInternet) {
            byPassSSL()
            client.post(url, params, responseHandler)
            //client.post(getAbsoluteUrl(url, context), params, responseHandler)
            println("post")
        }
    }

    fun post(context: Context, url: String, entity: HttpEntity, contentType: String, responseHandler: AsyncHttpResponseHandler) {
        if (GlobalVariable.hasInternet) {
            byPassSSL()
            client.post(context, url, entity, contentType, responseHandler)
            //client.post(context, getAbsoluteUrl(url, context), entity, contentType, responseHandler)
            println("post")
        }
    }

    private fun getAbsoluteUrl(relativeUrl: String, context: Context): String {

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val api = prefs.getString("pref_web_api", GlobalVariable.urlInternal)
        //IST: 172.16.27.157, LTLABS: 172.30.58.40, HOTEL: http://192.168.2.171, CLOUD: http://128.199.199.113
        val conn = prefs.getString("pref_conn_string", "")


        return "$api$conn$relativeUrl"

    }

    /*private fun getAbsoluteUrl(relativeUrl: String): String {
        return BASE_URL + relativeUrl
    }*/
}