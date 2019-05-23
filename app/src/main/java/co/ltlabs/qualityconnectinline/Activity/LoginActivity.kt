package co.ltlabs.qualityconnectinline.Activity

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.serialport.Application
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import com.loopj.android.http.JsonHttpResponseHandler
import org.apache.http.Header
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicHeader
import org.apache.http.protocol.HTTP
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import com.loopj.android.http.RequestParams
import androidx.appcompat.app.AlertDialog
import co.ltlabs.qualityconnectinline.*
import co.ltlabs.qualityconnectinline.Utility.API
import co.ltlabs.qualityconnectinline.Utility.GlobalVariable
import co.ltlabs.qualityconnectinline.Utility.NFCReader
import co.ltlabs.qualityconnectinline.Utility.NetworkStateReceiver
import kotlinx.android.synthetic.main.activity_login.*
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric


class LoginActivity : AppCompatActivity(), NetworkStateReceiver.NetworkStateReceiverListener {

    var nfcReader = NFCReader()

    var api = API()
    var userId = ""

    var isActivityVisible: Boolean = false
    var networkStateReceiver: NetworkStateReceiver? = null
    var progressDialogConnection: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_login)

        isActivityVisible = true
        progressDialogConnection = ProgressDialog.show(this@LoginActivity, getCaption("Reconnecting..."),
                        getCaption("You will not be able to save changes until your connection is restored."), true)
        progressDialogConnection!!.setCancelable(false)
        setNetworkStateReceiver()

        initializeComponent()
        nfcReader.run(this@LoginActivity, application as Application, mHandler1)

    }

    private fun setNetworkStateReceiver(){
        networkStateReceiver = NetworkStateReceiver(this@LoginActivity)
        networkStateReceiver!!.addListener(this@LoginActivity)
        applicationContext.registerReceiver(networkStateReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

    }

    override fun onNetworkAvailable() {
        GlobalVariable.hasInternet = true

        if (isActivityVisible) {
            populateLanguage()
            getTranslation("ENG", "ENG")
            progressDialogConnection!!.dismiss()
        }
    }

    override fun onNetworkUnavailable() {
        GlobalVariable.hasInternet = false

        if (isActivityVisible) {
            progressDialogConnection!!.show()
        }
    }

    private fun initializeComponent(){

        textInput_Username.hint = "User name/ Email Address"
        textInput_Password.hint = "Password"
        button_Login.text = "LOG IN"
        textView_CardReader.text = "or Tap the RFID/ NFC Card"

        if (!GlobalVariable.hasBuiltInReader) {
            textInput_Username.editText?.requestFocus()
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        }

        val packageInfo = this.packageManager.getPackageInfo(this.packageName, 0)
        textView_Version.text = "Version ${packageInfo.versionName}"

        /*if (!GlobalVariable.hasBuiltInReader) {
            textInput_Username.editText?.requestFocus()
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        }*/
        textInput_Username.editText?.requestFocus()
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        val fieldValidatorTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (textInput_Username.editText?.text.toString().length >= 10 && textInput_Username.editText?.text.toString().toIntOrNull() != null) {
                    validateUser(textInput_Username.editText?.text.toString())
                }
            }
        }
        textInput_Username.editText?.addTextChangedListener(fieldValidatorTextWatcher)

        button_Login.setOnClickListener {
            if (textInput_Username.editText?.length() == 0) {
                Toast.makeText(this, "Username/ Email is required!", Toast.LENGTH_SHORT).show()
            } else if (textInput_Password.editText?.length() == 0) {
                Toast.makeText(this, "Password is required!", Toast.LENGTH_SHORT).show()
            } else {
                validateUser("")
            }
        }

        populateLanguage()
        getTranslation("ENG", "ENG")
    }

    private fun populateLanguage() {

        val params = RequestParams()
        params.put("filter[fields][language]", "true")
        params.put("filter[fields][desc1]", "true")

        api.get(this,
            GlobalVariable.urlInternal + ":3006/api/languages", params, object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, response: JSONObject?) {}

            override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray?) {
                try {

                    GlobalVariable.languageList = emptyArray()
                    GlobalVariable.languageDescList = emptyArray()
                    for (i in 0..(timeline!!.length() - 1)) {

                        var array = arrayOf<String>()
                        for (j in 0..1) {
                            array += ""
                        }

                        GlobalVariable.languageList += array

                        GlobalVariable.languageList[i][0] = JSONObject(timeline[i].toString())["language"].toString()
                        GlobalVariable.languageList[i][1] = JSONObject(timeline[i].toString())["desc1"].toString()

                        GlobalVariable.languageDescList += JSONObject(timeline[i].toString())["desc1"].toString()
                    }

                    textView_Language.text = GlobalVariable.languageDescList[0]
                    GlobalVariable.currentLanguage = GlobalVariable.languageDescList[0]

                    floatingButton_Language.setOnClickListener {

                        val builder = AlertDialog.Builder(this@LoginActivity)
                        builder.setTitle("Choose language")

                        builder.setItems(GlobalVariable.languageDescList) { dialog, position ->
                            var prevLanguage: String = ""
                            for (array in GlobalVariable.languageList) {
                                if (array[1] == textView_Language.text) {
                                    prevLanguage = array[0]
                                    break
                                }
                            }

                            textView_Language.text = GlobalVariable.languageDescList[position]
                            GlobalVariable.currentLanguage = GlobalVariable.languageList[position][1]
                            getTranslation(prevLanguage, GlobalVariable.languageList[position][0])
                        }

                        val dialog = builder.create()
                        dialog.show()
                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                Toast.makeText(this@LoginActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                Toast.makeText(this@LoginActivity, responseString, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun getTranslation(prevLanguage: String, nextLanguage: String) {
        val jsonParams = JSONObject()
        jsonParams.put("product", GlobalVariable.product)
        jsonParams.put("previousLanguage", prevLanguage)
        jsonParams.put("nextLanguage", nextLanguage)


        val entity = StringEntity(jsonParams.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        api.post(this@LoginActivity, GlobalVariable.urlInternal + ":3001/api/v1/getLanguageTranslation",
                 entity,"application/json", object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, response: JSONObject) {}
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray) {
                try {
                    if (statusCode == 200) {

                        GlobalVariable.translationMap.clear()
                        var jsonObject: JSONObject
                        for (i in 0..(timeline.length() - 1)) {
                            jsonObject = JSONObject(timeline[i].toString())

                            GlobalVariable.translationMap.put(jsonObject["prevLang"].toString(), jsonObject["nextLang"].toString())
                        }

                        updateTranslationCaption()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                Toast.makeText(this@LoginActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                Toast.makeText(this@LoginActivity, responseString, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun updateTranslationCaption() {

        textInput_Username.hint = getCaption(textInput_Username.hint.toString())
        textInput_Password.hint = getCaption(textInput_Password.hint.toString())
        button_Login.text = getCaption(button_Login.text.toString())
        textView_CardReader.text = getCaption(textView_CardReader.text.toString())
    }

    private fun getCaption(caption: String): String {

        var value: String
        if (GlobalVariable.translationMap.containsKey(caption.toUpperCase())) {
            value = GlobalVariable.translationMap.get(caption.toUpperCase()).toString()
        } else {
            value = caption
        }

        return value
    }

    override fun onResume() {
        registerNetworkBroadcastReceiver(this@LoginActivity)
        super.onResume()
        nfcReader.onResume()
        isActivityVisible = true
    }

    override fun onPause() {
        unregisterNetworkBroadcastReceiver(this@LoginActivity)
        super.onPause()
        isActivityVisible = false
    }

    override fun onBackPressed() {
        finishAndRemoveTask()
    }

    @SuppressLint("HandlerLeak")
    var mHandler1 = object : Handler() {
        override fun handleMessage(msg: Message) {
            nfcReader.handleMessage(msg)

            if (nfcReader.uid.isNotEmpty()) validateUser(nfcReader.uid)
        }
    }

    fun validateUser(cardId: String) {

        var jsonParams = JSONObject()
        jsonParams.put("username", textInput_Username.editText?.text.toString())
        jsonParams.put("email", textInput_Username.editText?.text.toString())
        jsonParams.put("password", textInput_Password.editText?.text.toString())
        jsonParams.put("cardId", cardId)


        var entity: StringEntity = StringEntity(jsonParams.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        api.post(this, GlobalVariable.urlInternal + ":3006/api/users/login", entity, "application/json", object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, response: JSONObject?) {
                try {
                    if (statusCode == 200) {
                        GlobalVariable.accessToken = response!!.getString("id")
                        userId = response!!.getString("userId")
                        populateConfig()
                        populateDefects()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray?) {}

            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                Toast.makeText(this@LoginActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                Toast.makeText(this@LoginActivity, responseString, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun populateConfig() {

        var jsonParams = JSONObject()
        jsonParams.put("factory", GlobalVariable.factory)
        jsonParams.put("mfgLine", GlobalVariable.mfgLine)
        jsonParams.put("userId", userId)

        var entity: StringEntity = StringEntity(jsonParams.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        api.post(this, GlobalVariable.urlInternal + ":3001/api/v1/qualityconnect/getConfigSP",
            entity, "application/json", object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray?) {
                try {
                    if (statusCode == 200) {
                        var jsonObject = JSONObject(JSONArray(timeline.toString())[0].toString())
                        /*GlobalVariable.apiOutput = jsonObject["apiOutput"].toString()
                        GlobalVariable.apiQC = jsonObject["apiQC"].toString()*/
                        GlobalVariable.userId = jsonObject["userName"].toString()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                Toast.makeText(this@LoginActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                Toast.makeText(this@LoginActivity, responseString, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun populateDefects() {

        /*val params = RequestParams()
        params.put("filter[include]", "defects")
        params.put("filter[fields][defectCat]", "true")
        params.put("filter[fields][id]", "true")
        params.put("filter[fields][desc1]", "true")
        params.put("access_token", GlobalVariable.accessToken)

        api.get(this, GlobalVariable.urlInternal + ":3006/api/defectscats?", params, object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, response: JSONObject?) {}

            override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray?) {
                try {
                    GlobalVariable.defectStringList = timeline.toString()

                    if (GlobalVariable.defectStringList.isNotEmpty()) {
                        val intent = Intent(this@LoginActivity, InlineLoadOnActivity::class.java)
                        startActivity(intent)
                        nfcReader.stop()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                println("$statusCode - $errorResponse")
                var errorMessage = JSONObject(errorResponse.getString("error")).getString("message")
                Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                println("$statusCode - $responseString")
            }
        })*/

        var jsonParams = JSONObject()
        jsonParams.put("factory", GlobalVariable.factory)

        var entity: StringEntity = StringEntity(jsonParams.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        api.post(this, GlobalVariable.urlInternal + ":3001/api/v1/qualityconnect/getDefectsCatTypesQCSP",
            entity, "application/json", object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray?) {
                try {
                    if (statusCode == 200) {
                        GlobalVariable.defectStringList = timeline.toString()

                        if (GlobalVariable.defectStringList.isNotEmpty()) {
                            val intent = Intent(this@LoginActivity, InlineLoadOnActivity::class.java)
                            startActivity(intent)
                            nfcReader.stop()
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                Toast.makeText(this@LoginActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                Toast.makeText(this@LoginActivity, responseString, Toast.LENGTH_LONG).show()
            }
        })
    }

    fun registerNetworkBroadcastReceiver(context: Context) {
        context.registerReceiver(networkStateReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    fun unregisterNetworkBroadcastReceiver(context: Context) {
        context.unregisterReceiver(networkStateReceiver)
    }
}
