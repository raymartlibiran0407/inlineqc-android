package co.ltlabs.qualityconnectinline.Activity

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_inline_load_on.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import co.ltlabs.qualityconnectinline.*
import co.ltlabs.qualityconnectinline.Adapter.CardStatusAdapter
import co.ltlabs.qualityconnectinline.Data.CardStatusList
import co.ltlabs.qualityconnectinline.Utility.API
import co.ltlabs.qualityconnectinline.Utility.GlobalVariable
import co.ltlabs.qualityconnectinline.Utility.NetworkStateReceiver
import com.loopj.android.http.JsonHttpResponseHandler
import com.loopj.android.http.RequestParams
import kotlinx.android.synthetic.main.activity_inline_load_on.floatingButton_Language
import kotlinx.android.synthetic.main.activity_inline_load_on.textView_Language
import kotlinx.android.synthetic.main.activity_login.*
import org.apache.http.Header
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicHeader
import org.apache.http.protocol.HTTP
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric


class InlineLoadOnActivity : AppCompatActivity(), NetworkStateReceiver.NetworkStateReceiverListener {

    var api = API()
    var cardStatusList = arrayOf<Array<String>>()
    var machineCode = ""
    var machinesList = arrayOf<Array<String>>()
    var operationCode = ""
    var operationsList = arrayOf<Array<String>>()
    var progressionTicketList = arrayOf<Array<String>>()
    var workersList = arrayOf<Array<String>>()

    var isActivityVisible: Boolean = false
    var networkStateReceiver: NetworkStateReceiver? = null
    var progressDialogConnection: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_inline_load_on)

        isActivityVisible = true
        progressDialogConnection = ProgressDialog.show(this@InlineLoadOnActivity, getCaption("Reconnecting..."),
            getCaption("You will not be able to save changes until your connection is restored."), true)
        progressDialogConnection!!.setCancelable(false)
        setNetworkStateReceiver()

        initializeComponent()

        getCardStatusList()
        updateTranslationCaption()
    }

    private fun setNetworkStateReceiver(){
        networkStateReceiver = NetworkStateReceiver(this@InlineLoadOnActivity)
        networkStateReceiver!!.addListener(this@InlineLoadOnActivity)
        applicationContext.registerReceiver(networkStateReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

    }

    override fun onNetworkAvailable() {
        GlobalVariable.hasInternet = true

        if (isActivityVisible) {
            progressDialogConnection!!.dismiss()
        }
    }

    override fun onNetworkUnavailable() {
        GlobalVariable.hasInternet = false

        if (isActivityVisible) {
            progressDialogConnection!!.show()
        }
    }

    /*override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        // fetch the user selected value
        val item = parent.getItemAtPosition(position).toString()

        // create Toast with user selected value
        Toast.makeText(this@InlineLoadOnActivity, "Selected Item is: \t$item", Toast.LENGTH_LONG).show()

    }*/

    private fun initializeComponent() {

        button_Clear.setOnClickListener {
            autoTextView_WorkersId.setText("")
            autoTextView_WorkersCardNo.setText("")
            textView_WorkersName.text = ""
            autoTextView_PTNo.setText("")
            autoTextView_PTCardNo.setText("")
            autoTextView_Operation.setText("")
            autoTextView_Machine.setText("")
        }

        button_Submit.setOnClickListener {

            if (autoTextView_WorkersId.text.isNotEmpty() &&
                autoTextView_WorkersCardNo.text.isNotEmpty() &&
                autoTextView_PTNo.text.isNotEmpty() &&
                //autoTextView_PTCardNo.text.isNotEmpty() &&
                autoTextView_Operation.text.isNotEmpty() &&
                autoTextView_Machine.text.isNotEmpty()) {

                var jsonParams = JSONObject()
                jsonParams.put("factory", GlobalVariable.factory)
                jsonParams.put("mfgLine", GlobalVariable.mfgLine)
                jsonParams.put("empId", autoTextView_WorkersId.text.toString())
                jsonParams.put("operation", operationCode)
                jsonParams.put("machine", autoTextView_Machine.text.toString())

                var entity: StringEntity = StringEntity(jsonParams.toString())
                entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

                api.post(this@InlineLoadOnActivity, GlobalVariable.urlInternal + ":3001/api/v1/qualityconnect/checkReachMaxInlineRejectSP",
                    entity, "application/json", object : JsonHttpResponseHandler() {
                    override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray) {
                        try {
                            if (statusCode == 200) {

                                var result = JSONObject(timeline[0].toString())["result"].toString()

                                if (result == "proceed") {
                                    val intent = Intent(this@InlineLoadOnActivity, DefectActivity::class.java)
                                    intent.putExtra("workerId", autoTextView_WorkersId.text.toString())
                                    intent.putExtra("ptNo", autoTextView_PTNo.text.toString())
                                    intent.putExtra("operationCode", operationCode)
                                    intent.putExtra("machineCode", autoTextView_Machine.text.toString())
                                    intent.putExtra("batchNoIncrement", 0)
                                    intent.putExtra("inspectionCountReset", false)
                                    startActivity(intent)
                                }
                                else if (result == "authorize") {

                                    val builder = AlertDialog.Builder(this@InlineLoadOnActivity)
                                    builder.setMessage(getCaption("Max card reject count reached. This will reset card status. Proceed?"))
                                    builder.setCancelable(true)

                                    builder.setNegativeButton(
                                        getCaption("No"),
                                        DialogInterface.OnClickListener { dialog, id -> dialog.cancel()})

                                    builder.setPositiveButton(
                                        getCaption("Yes"),
                                        DialogInterface.OnClickListener { dialog, id -> dialog.cancel()
                                            val intent = Intent(this@InlineLoadOnActivity, DefectActivity::class.java)
                                            intent.putExtra("workerId", autoTextView_WorkersId.text.toString())
                                            intent.putExtra("ptNo", autoTextView_PTNo.text.toString())
                                            intent.putExtra("operationCode", operationCode)
                                            intent.putExtra("machineCode", autoTextView_Machine.text.toString())
                                            intent.putExtra("batchNoIncrement", 1)
                                            intent.putExtra("inspectionCountReset", true)
                                            startActivity(intent)
                                        }
                                    )

                                    val alert = builder.create()
                                    alert.show()

                                    /*val intent = Intent(this@InlineLoadOnActivity, AuthorizationActivity::class.java)
                                    intent.putExtra("workerId", autoTextView_WorkersId.text.toString())
                                    intent.putExtra("ptNo", autoTextView_PTNo.text.toString())
                                    intent.putExtra("operationCode", operationCode)
                                    intent.putExtra("machineCode", autoTextView_Machine.text.toString())
                                    startActivity(intent)*/
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                        Toast.makeText(this@InlineLoadOnActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
                    }

                    override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                        Toast.makeText(this@InlineLoadOnActivity, responseString, Toast.LENGTH_LONG).show()
                    }
                })
            } else {
                Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
            }
        }

        populateWorker()
        populateProgressionTicket()

        autoTextView_WorkersId.clearFocus()
        autoTextView_WorkersCardNo.clearFocus()
        autoTextView_PTNo.clearFocus()
        autoTextView_PTCardNo.clearFocus()
        autoTextView_Operation.clearFocus()
        autoTextView_Machine.clearFocus()

        autoTextView_WorkersId.setOnClickListener {
            autoTextView_WorkersId.showDropDown()
        }

        autoTextView_WorkersCardNo.setOnClickListener {
            autoTextView_WorkersCardNo.showDropDown()
        }

        autoTextView_PTNo.setOnClickListener {
            autoTextView_PTNo.showDropDown()
        }

        autoTextView_PTCardNo.setOnClickListener {
            autoTextView_PTCardNo.showDropDown()
        }

        /*val operationTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                operationCode = autoTextView_Operation.text.toString()
            }
        }
        autoTextView_Operation?.addTextChangedListener(operationTextWatcher)*/

        autoTextView_Operation.setOnClickListener {
            populateOperation()
        }

        autoTextView_Machine.setOnClickListener {
            populateMachine()
        }

        /*autoTextView_WorkersId.setOnItemClickListener { parent, view, position, id ->
            autoTextView_WorkersCardNo.setText(workersList[id.toInt()][2])
            textView_WorkersName.text = workersList[id.toInt()][3]
        }*/

        val workersIdTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                for (i in 0 until workersList.size - 1) {
                    if (workersList[i][1] == autoTextView_WorkersId.text.toString()) {
                        //autoTextView_WorkersId.setText(workersList[i][1])
                        autoTextView_WorkersCardNo.setText(workersList[i][2])
                        textView_WorkersName.text = workersList[i][3]
                        break
                    }
                }
            }
        }
        autoTextView_WorkersId?.addTextChangedListener(workersIdTextWatcher)

        /*autoTextView_WorkersCardNo.setOnItemClickListener { parent, view, position, id ->
            autoTextView_WorkersId.setText(workersList[id.toInt()][1])
            textView_WorkersName.text = workersList[id.toInt()][3]
        }*/

        /*val workersCardNoTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                for (i in 0 until workersList.size - 1) {
                    if (workersList[i][2] == autoTextView_WorkersCardNo.text.toString()) {
                        autoTextView_WorkersId.setText(workersList[i][1])
                        //autoTextView_WorkersCardNo.setText(workersList[i][2])
                        textView_WorkersName.text = workersList[i][3]
                        break
                    }
                }
            }
        }
        autoTextView_WorkersCardNo?.addTextChangedListener(workersCardNoTextWatcher)*/

        /*autoTextView_PTNo.setOnItemClickListener { parent, view, position, id ->
            autoTextView_PTCardNo.setText(progressionTicketList[id.toInt()][3])
            GlobalVariable.mfgLine = progressionTicketList[id.toInt()][1]
        }*/

        val ptNoTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                for (i in 0 until progressionTicketList.size - 1) {
                    if (progressionTicketList[i][2] == autoTextView_PTNo.text.toString()) {
                        autoTextView_PTCardNo.setText(progressionTicketList[i][3])
                        GlobalVariable.mfgLine = progressionTicketList[i][1]
                        break
                    }
                }
            }
        }
        autoTextView_PTNo?.addTextChangedListener(ptNoTextWatcher)

        /*autoTextView_PTCardNo.setOnItemClickListener { parent, view, position, id ->
            autoTextView_PTNo.setText(progressionTicketList[id.toInt()][2])
            GlobalVariable.mfgLine = progressionTicketList[id.toInt()][1]
        }*/

       /* val ptCardNoTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                for (i in 0 until progressionTicketList.size - 1) {
                    if (progressionTicketList[i][3] == autoTextView_PTCardNo.text.toString()) {
                        autoTextView_PTNo.setText(progressionTicketList[i][2])
                        GlobalVariable.mfgLine = progressionTicketList[i][1]
                        break
                    }
                }
            }
        }
        autoTextView_PTCardNo?.addTextChangedListener(ptCardNoTextWatcher)*/

        /*autoTextView_Operation.setOnItemClickListener { parent, view, position, id ->
            operationCode = operationsList[id.toInt()][1]
        }*/

        val operationTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                for (i in 0 until operationsList.size - 1) {
                    if (operationsList[i][2] == autoTextView_Operation.text.toString()) {
                        operationCode = operationsList[i][1]
                        break
                    }
                }
            }
        }
        autoTextView_Operation?.addTextChangedListener(operationTextWatcher)

        populateLanguage()
        postLogout()

        /*GlobalVariable.mfgLine = "YTI-10"
        autoTextView_WorkersId.setText("C000025")
        autoTextView_WorkersCardNo.setText("C000025")
        autoTextView_PTNo.setText("STX2000460926")
        autoTextView_PTCardNo.setText("")
        autoTextView_Operation.setText("testingOperation")
        operationCode = "testingOperation"
        autoTextView_Machine.setText("testingMachine")*/
    }

    private fun populateLanguage() {

        floatingButton_Language.setOnClickListener {view ->

            val builder = AlertDialog.Builder(this@InlineLoadOnActivity)
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

        textView_Language.text = GlobalVariable.currentLanguage
    }

    private fun postLogout() {
        floatingButton_Logout.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(getCaption("Do you want logout?"))
            builder.setCancelable(true)

            builder.setNegativeButton(
                getCaption("No"),
                DialogInterface.OnClickListener { dialog, id -> dialog.cancel()})

            builder.setPositiveButton(
                getCaption("Yes"),
                DialogInterface.OnClickListener { dialog, id -> dialog.cancel()
                    var intent = Intent(this@InlineLoadOnActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            )

            val alert = builder.create()
            alert.show()
        }
    }

    private fun getTranslation(prevLanguage: String, nextLanguage: String) {
        val jsonParams = JSONObject()
        jsonParams.put("product", GlobalVariable.product)
        jsonParams.put("previousLanguage", prevLanguage)
        jsonParams.put("nextLanguage", nextLanguage)

        val entity = StringEntity(jsonParams.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        api.post(this@InlineLoadOnActivity, GlobalVariable.urlInternal + ":3001/api/v1/getLanguageTranslation",
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
                Toast.makeText(this@InlineLoadOnActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                Toast.makeText(this@InlineLoadOnActivity, responseString, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun updateTranslationCaption() {

        textView_WorkerId.text = getCaption(textView_WorkerId.text.toString())
        textView_WorkerCardNo.text = getCaption(textView_WorkerCardNo.text.toString())
        textView_PTNo.text = getCaption(textView_PTNo.text.toString())
        textView_PTCardNo.text = getCaption(textView_PTCardNo.text.toString())
        textView_Operation.text = getCaption(textView_Operation.text.toString())
        textView_Machine.text = getCaption(textView_Machine.text.toString())
        textView_CardStatus.hint = getCaption(textView_CardStatus.text.toString())

        button_Clear.text = getCaption(button_Clear.text.toString())
        button_Submit.text = getCaption(button_Submit.text.toString())
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

    private fun populateWorker() {
        var jsonParams = JSONObject()
        jsonParams.put("factory", GlobalVariable.factory)
        jsonParams.put("mfgLine", GlobalVariable.mfgLineMultiple)
        jsonParams.put("ptNo", "")
        jsonParams.put("operation", "")
        jsonParams.put("dropdownCat", "workers")

        var entity: StringEntity = StringEntity(jsonParams.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        api.post(this, GlobalVariable.urlInternal + ":3001/api/v1/qualityconnect/getInlineQCDropdownDataSP",
            entity, "application/json", object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray?) {
                try {

                    var jsonObject: JSONObject
                    var workersCardNoList = arrayOf<String>()
                    var workersIdList = arrayOf<String>()

                    for (i in 0..(timeline!!.length() - 1)) {

                        var array = arrayOf<String>()
                        for (j in 0..3) {
                            array += ""
                        }
                        workersList += array
                        jsonObject = JSONObject(timeline[i].toString())

                        workersList[i][0] = i.toString()
                        workersList[i][1] = jsonObject["empId"].toString() //empId
                        workersList[i][2] = jsonObject["cardNo"].toString() //cardNo
                        workersList[i][3] = jsonObject["fullName"].toString() //fullName

                        workersIdList += jsonObject["empId"].toString()
                        workersCardNoList += jsonObject["cardNo"].toString()
                    }

                    ArrayAdapter<String>(this@InlineLoadOnActivity, android.R.layout.simple_list_item_1, workersIdList).also {
                            adapter -> autoTextView_WorkersId.setAdapter(adapter)}

                    ArrayAdapter<String>(this@InlineLoadOnActivity, android.R.layout.simple_list_item_1, workersCardNoList).also {
                            adapter -> autoTextView_WorkersCardNo.setAdapter(adapter)}

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                Toast.makeText(this@InlineLoadOnActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                Toast.makeText(this@InlineLoadOnActivity, responseString, Toast.LENGTH_LONG).show()
            }
        })

        autoTextView_WorkersId.threshold = 1
        autoTextView_WorkersCardNo.threshold = 1
    }

    private fun populateProgressionTicket() {
        var jsonParams = JSONObject()
        jsonParams.put("factory", GlobalVariable.factory)
        jsonParams.put("mfgLine", GlobalVariable.mfgLineMultiple)
        jsonParams.put("ptNo", "")
        jsonParams.put("operation", "")
        jsonParams.put("dropdownCat", "ptNo")

        var entity: StringEntity = StringEntity(jsonParams.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        api.post(this, GlobalVariable.urlInternal + ":3001/api/v1/qualityconnect/getInlineQCDropdownDataSP",
            entity, "application/json", object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray?) {
                try {

                    var jsonObject: JSONObject
                    var ptCardNoList = arrayOf<String>()
                    var ptNoList = arrayOf<String>()

                    for (i in 0..(timeline!!.length() - 1)) {

                        var array = arrayOf<String>()
                        for (j in 0..3) {
                            array += ""
                        }
                        progressionTicketList += array
                        jsonObject = JSONObject(timeline[i].toString())

                        progressionTicketList[i][0] = i.toString()
                        progressionTicketList[i][1] = jsonObject["PRODLINE"].toString() //PRODLINE
                        progressionTicketList[i][2] = jsonObject["PTNO"].toString() //PTNO
                        progressionTicketList[i][3] = jsonObject["CARDNO"].toString() //CARDNO

                        ptNoList += jsonObject["PTNO"].toString()
                        ptCardNoList += jsonObject["CARDNO"].toString()
                    }

                    ArrayAdapter<String>(this@InlineLoadOnActivity, android.R.layout.simple_list_item_1, ptNoList).also {
                            adapter -> autoTextView_PTNo.setAdapter(adapter)}

                    ArrayAdapter<String>(this@InlineLoadOnActivity, android.R.layout.simple_list_item_1, ptCardNoList).also {
                            adapter -> autoTextView_PTCardNo.setAdapter(adapter)}

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                Toast.makeText(this@InlineLoadOnActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                Toast.makeText(this@InlineLoadOnActivity, responseString, Toast.LENGTH_LONG).show()
            }
        })

        autoTextView_PTNo.threshold = 1
        autoTextView_PTCardNo.threshold = 1
    }

    private fun populateOperation() {
        var jsonParams = JSONObject()
        jsonParams.put("factory", GlobalVariable.factory)
        jsonParams.put("mfgLine", GlobalVariable.mfgLineMultiple)
        jsonParams.put("ptNo", autoTextView_PTNo.text.toString())
        jsonParams.put("operation", "")
        jsonParams.put("dropdownCat", "operations")

        var entity: StringEntity = StringEntity(jsonParams.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        api.post(this, GlobalVariable.urlInternal + ":3001/api/v1/qualityconnect/getInlineQCDropdownDataSP",
            entity, "application/json", object : JsonHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray?) {
                    try {

                        var jsonObject: JSONObject
                        var operationDescList = arrayOf<String>()

                        for (i in 0..(timeline!!.length() - 1)) {

                            var array = arrayOf<String>()
                            for (j in 0..2) {
                                array += ""
                            }
                            operationsList += array
                            jsonObject = JSONObject(timeline[i].toString())

                            operationsList[i][0] = i.toString()
                            operationsList[i][1] = jsonObject["operation"].toString()
                            operationsList[i][2] = jsonObject["desc1"].toString()

                            operationDescList += jsonObject["desc1"].toString()
                        }

                        ArrayAdapter<String>(this@InlineLoadOnActivity, android.R.layout.simple_list_item_1, operationDescList).also {
                                adapter -> autoTextView_Operation.setAdapter(adapter)}

                        autoTextView_Operation.showDropDown()

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                    Toast.makeText(this@InlineLoadOnActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
                }

                override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                    Toast.makeText(this@InlineLoadOnActivity, responseString, Toast.LENGTH_LONG).show()
                }
            })

        autoTextView_Operation.threshold = 1
    }

    private fun populateMachine() {
        var jsonParams = JSONObject()
        jsonParams.put("factory", GlobalVariable.factory)
        jsonParams.put("mfgLine", GlobalVariable.mfgLineMultiple)
        jsonParams.put("ptNo", autoTextView_PTNo.text.toString())
        jsonParams.put("operation", operationCode)
        jsonParams.put("dropdownCat", "machines")

        var entity: StringEntity = StringEntity(jsonParams.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        api.post(this, GlobalVariable.urlInternal + ":3001/api/v1/qualityconnect/getInlineQCDropdownDataSP",
            entity, "application/json", object : JsonHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray?) {
                    try {

                        var jsonObject: JSONObject
                        var machineDescList = arrayOf<String>()

                        for (i in 0..(timeline!!.length() - 1)) {

                            var array = arrayOf<String>()
                            for (j in 0..2) {
                                array += ""
                            }
                            machinesList += array
                            jsonObject = JSONObject(timeline[i].toString())

                            machinesList[i][0] = i.toString()
                            machinesList[i][1] = jsonObject["machine"].toString()

                            machineDescList += jsonObject["machine"].toString()
                        }

                        ArrayAdapter<String>(this@InlineLoadOnActivity, android.R.layout.simple_list_item_1, machineDescList).also {
                                adapter -> autoTextView_Machine.setAdapter(adapter)}

                        autoTextView_Machine.showDropDown()

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                    Toast.makeText(this@InlineLoadOnActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
                }

                override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                    Toast.makeText(this@InlineLoadOnActivity, responseString, Toast.LENGTH_LONG).show()
                }
            })

        autoTextView_Machine.threshold = 1
    }

    private fun getCardStatusList() {
        var jsonParams = JSONObject()
        jsonParams.put("factory", GlobalVariable.factory)
        jsonParams.put("mfgLine", GlobalVariable.mfgLineMultiple)

        var entity: StringEntity = StringEntity(jsonParams.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        api.post(this, GlobalVariable.urlInternal + ":3001/api/v1/qualityconnect/getInlineCardStatusSP",
            entity, "application/json", object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray?) {
                try {
                    var jsonObject: JSONObject

                    for (i in 0..(timeline!!.length() - 1)) {
                        var array = arrayOf<String>()
                        for (j in 0..7) {
                            array += ""
                        }

                        cardStatusList += array
                        jsonObject = JSONObject(timeline[i].toString())

                        cardStatusList[i][0] = i.toString()
                        cardStatusList[i][1] = jsonObject["empId"].toString()
                        cardStatusList[i][2] = jsonObject["empName"].toString()
                        cardStatusList[i][3] = jsonObject["operation"].toString()
                        cardStatusList[i][4] = jsonObject["operationDesc"].toString()
                        cardStatusList[i][5] = jsonObject["machine"].toString()
                        cardStatusList[i][6] = jsonObject["rejectCount"].toString()
                        cardStatusList[i][7] = jsonObject["color"].toString()
                    }

                    populateRejectList()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                Toast.makeText(this@InlineLoadOnActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                Toast.makeText(this@InlineLoadOnActivity, responseString, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun populateRejectList() {

        var rejectAdapter = CardStatusAdapter(
            applicationContext,
            R.layout.list_item_card_status
        )

        for (array in cardStatusList) {
            val rejectList = CardStatusList(
                getCaption("Operator") + ": ${array[2]}",
                getCaption("Operation") + ": ${array[4]}",
                getCaption("Machine") + ": ${array[5]}",
                getCaption("Reject Count") + ": ${array[6]}",
                array[7]
            )
            rejectAdapter.add(rejectList)
        }

        listView_CardStatus.adapter = rejectAdapter

        listView_CardStatus.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            populateInfo(id.toInt())
            //populateRejects(cardStatusList[id.toInt()][3])
        }
    }

    private fun populateInfo(id: Int) {

        for (i in 0 until workersList.size - 1) {
            if (workersList[i][1] == cardStatusList[id][1]) {
                autoTextView_WorkersId.setText(workersList[i][1])
                autoTextView_WorkersCardNo.setText(workersList[i][2])
                textView_WorkersName.text = workersList[i][3]
                break
            }
        }

        autoTextView_Machine.setText(cardStatusList[id][5])
    }

    private fun exitPage() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getCaption("Do you want to exit current page?"))
        builder.setCancelable(true)

        builder.setNegativeButton(
            getCaption("No"),
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel()})

        builder.setPositiveButton(
            getCaption("Yes"),
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel()
                val intent = Intent(this@InlineLoadOnActivity, LoginActivity::class.java)
                startActivity(intent)
            }
        )

        val alert = builder.create()
        alert.show()
    }

    override fun onResume() {
        registerNetworkBroadcastReceiver(this@InlineLoadOnActivity)
        super.onResume()
        isActivityVisible = true
    }

    override fun onPause() {
        unregisterNetworkBroadcastReceiver(this@InlineLoadOnActivity)
        super.onPause()
        finish()
        isActivityVisible = false
    }

    override fun onBackPressed() {
        exitPage()
    }

    fun registerNetworkBroadcastReceiver(context: Context) {
        context.registerReceiver(networkStateReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    fun unregisterNetworkBroadcastReceiver(context: Context) {
        context.unregisterReceiver(networkStateReceiver)
    }
}
