package co.ltlabs.qualityconnectinline.Activity

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.widget.*
import android.content.DialogInterface
import android.content.Intent
import android.view.*
import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.IntentFilter
import android.graphics.PorterDuff
import android.net.ConnectivityManager
import android.widget.Toast
import com.loopj.android.http.JsonHttpResponseHandler
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import android.view.Gravity
import co.ltlabs.qualityconnectinline.*
import co.ltlabs.qualityconnectinline.Utility.API
import co.ltlabs.qualityconnectinline.Utility.GlobalVariable
import co.ltlabs.qualityconnectinline.Utility.NetworkStateReceiver
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_defect.*
import kotlinx.android.synthetic.main.activity_defect.textView_PTNo
import org.apache.http.Header
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicHeader
import org.apache.http.protocol.HTTP
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

class DefectActivity : AppCompatActivity(), NetworkStateReceiver.NetworkStateReceiverListener {

    var api = API()
    var batchNoIncrement = 0
    var bdlNo: String = ""
    var countAllReject: Int = 0
    var currentCardCount: Int = 0
    var currentCardStatus: String = ""
    var defectList = arrayOf<Array<String>>()
    var inspectionCountReset: Boolean = false
    var machineCode: String = ""
    var maxCardCount: Int = 0
    var maxCardStatus: String = ""
    var operationCode: String = ""
    var ptNo: String = ""
    var rejectList = arrayOf<Array<String>>()
    var validateCardResult: String = ""
    var workerId: String = ""

    var pCreatedBy: String = ""
    var pBatchNo: String = ""
    var pLayNo: String = ""
    var pMONo: String = ""
    var pPTNo: String = ""
    var pQty: Int = 0
    var pSeq: String = ""

    var isActivityVisible: Boolean = false
    var networkStateReceiver: NetworkStateReceiver? = null
    var progressDialogConnection: ProgressDialog? = null

    private var buttonStyle: Int = R.style.Widget_MaterialComponents_Button
    private var buttonBackgroundColor: Int = R.color.global_button_background_color
    private var buttonBackgroundColorDefectCategoryClicked: Int =
        R.color.holo_orange_dark
    private var buttonBackgroundColorDefectTypeIncrementClicked: Int =
        R.color.holo_red_light

    private var dialogReject: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_defect)

        isActivityVisible = true
        progressDialogConnection = ProgressDialog.show(this@DefectActivity, getCaption("Reconnecting..."),
            getCaption("You will not be able to save changes until your connection is restored."), true)
        progressDialogConnection!!.setCancelable(false)
        setNetworkStateReceiver()

        var intent: Intent = intent
        workerId = intent.getStringExtra("workerId")
        ptNo = intent.getStringExtra("ptNo")
        operationCode = intent.getStringExtra("operationCode")
        machineCode = intent.getStringExtra("machineCode")
        batchNoIncrement = intent.getIntExtra("batchNoIncrement", 0)
        inspectionCountReset = intent.getBooleanExtra("inspectionCountReset", false)

        initializeComponent()

        getHeaderList()
        getDefectList()
        updateTranslationCaption()
        populateCategory()

        if (defectList.isNotEmpty()) {
            populateTypes(defectList[0][0])
        }
    }

    private fun setNetworkStateReceiver(){
        networkStateReceiver = NetworkStateReceiver(this@DefectActivity)
        networkStateReceiver!!.addListener(this@DefectActivity)
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

    private fun initializeComponent() {

        button_Exit.setOnClickListener {
            exitPage()
        }

        button_Reset.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(getCaption("Do you want to reset defect/s count to zero?"))
            builder.setCancelable(true)

            builder.setNegativeButton(
                getCaption("No"),
                DialogInterface.OnClickListener { dialog, id -> dialog.cancel()})

            builder.setPositiveButton(
                getCaption("Yes"),
                DialogInterface.OnClickListener { dialog, id -> dialog.cancel()
                    resetDefects()
                }
            )

            val alert = builder.create()
            alert.show()
        }

        button_Next.setOnClickListener {
            if (countAllReject > 0) {
                postReject()
            } else {
                postGood()
            }
            /*val intent = Intent(this@DefectActivity, AuthorizationActivity::class.java)
            startActivity(intent)*/
        }

        populateLanguage()
        postLogout()
    }

    private fun populateLanguage() {

        floatingButton_Language.setOnClickListener {

            val builder = AlertDialog.Builder(this@DefectActivity)
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
                    var intent = Intent(this@DefectActivity, LoginActivity::class.java)
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

        api.post(this@DefectActivity, GlobalVariable.urlInternal + ":3001/api/v1/getLanguageTranslation",
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
                        populateCategory()
                        populateTypes(defectList[0][0])
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                Toast.makeText(this@DefectActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                Toast.makeText(this@DefectActivity, responseString, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun updateTranslationCaption() {

        button_Exit.text = getCaption(button_Exit.text.toString())
        button_Reset.text = getCaption(button_Reset.text.toString())
        button_Next.text = getCaption(button_Next.text.toString())

        textView_MONoCol.text = getCaption(textView_MONoCol.text.toString())
        textView_IONoCol.text = getCaption(textView_IONoCol.text.toString())
        textView_PONoCol.text = getCaption(textView_PONoCol.text.toString())
        textView_CardDefectStatusCol.text = getCaption(textView_CardDefectStatusCol.text.toString())
        textView_ColorCol.text = getCaption(textView_ColorCol.text.toString())
        textView_LayNoCol.text = getCaption(textView_LayNoCol.text.toString())
        textView_SizeCol.text = getCaption(textView_SizeCol.text.toString())
        textView_BundleQtyCol.text = getCaption(textView_BundleQtyCol.text.toString())
        textView_PTNoCol.text = getCaption(textView_PTNoCol.text.toString())
        textView_BatchNoCol.text = getCaption(textView_BatchNoCol.text.toString())
        textView_InspectionCountCol.text = getCaption(textView_InspectionCountCol.text.toString())
        textView_TopDefectOneCol.text = getCaption(textView_TopDefectOneCol.text.toString())
        textView_TopDefectTwoCol.text = getCaption(textView_TopDefectTwoCol.text.toString())
        textView_TopDefectThreeCol.text = getCaption(textView_TopDefectThreeCol.text.toString())
        textView_RejectGoodCol.text = getCaption(textView_RejectGoodCol.text.toString())

        /*Defect Array Value
        defectList[0][0] = "Cleanliness" // Defect Category Desc
        defectList[0][1] = "Dirt Stain" // Defect Type Desc
        defectList[0][2] = "0" // Defect Type Value
        defectList[0][3] = "1" // Defect Category Id
        defectList[0][4] = "C01" // Defect Type Id*/

        for (array in defectList) {
            array[0] = getCaption(array[0])
            array[1] = getCaption(array[1])
        }
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

    private fun exitPage() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getCaption("Do you want to exit defects page?"))
        builder.setCancelable(true)

        builder.setNegativeButton(
            getCaption("No"),
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel()})

        builder.setPositiveButton(
            getCaption("Yes"),
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel()
                val intent = Intent(this@DefectActivity, InlineLoadOnActivity::class.java)
                startActivity(intent)
            }
        )

        val alert = builder.create()
        alert.show()
    }

    private fun getHeaderList() {

        var jsonParams = JSONObject()
        jsonParams.put("factory", GlobalVariable.factory)
        jsonParams.put("mfgLine", GlobalVariable.mfgLine)
        jsonParams.put("empId", workerId)
        jsonParams.put("ptNo", ptNo)
        jsonParams.put("operation", operationCode)
        jsonParams.put("machine", machineCode)

        var entity: StringEntity = StringEntity(jsonParams.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        api.post(this, GlobalVariable.urlInternal + ":3001/api/v1/qualityconnect/getInlineQCHeaderSP",
            entity, "application/json", object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, response: JSONObject) {}

            override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray) {
                try {
                    if (statusCode == 200) {
                        var jsonObject = JSONObject(timeline[0].toString())

                        currentCardStatus = jsonObject["cardStatus"].toString()
                        currentCardCount = jsonObject["rCount"].toString().toInt()
                        maxCardStatus = jsonObject["maxCardStatus"].toString()
                        maxCardCount = jsonObject["maxCardCount"].toString().toInt()

                        textView_MONo.text = jsonObject["moNo"].toString()
                        textView_IONo.text = jsonObject["ioNo"].toString()
                        textView_PONo.text = jsonObject["poNo"].toString()
                        textView_CardDefectStatus!!.background.setColorFilter(
                            Color.parseColor(jsonObject["cardStatus"].toString()),
                            PorterDuff.Mode.SRC_ATOP
                        )
                        textView_Color.text = jsonObject["color"].toString()
                        textView_LayNo.text = jsonObject["layNo"].toString()
                        bdlNo = jsonObject["bdlNo"].toString()
                        textView_Size.text = jsonObject["size"].toString()
                        textView_TotalDefects.text = jsonObject["totalDefects"].toString()
                        textView_BundleQty.text = jsonObject["loadOnQty"].toString()
                        textView_PTNo.text = jsonObject["ptNo"].toString()
                        textView_BatchNo.text = (jsonObject["batchNo"].toString().toInt() + batchNoIncrement).toString()
                        textView_InspectionCount.text = (if (inspectionCountReset) "0" else jsonObject["seq"].toString())
                        textView_TopDefectOne.text = jsonObject["top1Defect"].toString()
                        textView_TopDefectTwo.text = jsonObject["top2Defect"].toString()
                        textView_TopDefectThree.text = jsonObject["top3Defect"].toString()
                        textView_RejectGood.text = jsonObject["rejectOverGood"].toString()

                        Picasso.get().load(jsonObject["styleImageLoc"].toString()).into(imageView_StyleImage)

                        batchNoIncrement = 0
                        inspectionCountReset = false
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                Toast.makeText(this@DefectActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                Toast.makeText(this@DefectActivity, responseString, Toast.LENGTH_LONG).show()
            }
        })

    }

    private fun getDefectList() {

        var timeline: JSONArray? = null
        var rowIndex = 0
        var jsonObject: JSONObject

        rowIndex = 0

        if (GlobalVariable.defectStringList.isNotEmpty()) {
            timeline = JSONArray(GlobalVariable.defectStringList)

            /*Defect Array Value
            defectList[0][0] = "Cleanliness" // Defect Category Desc
            defectList[0][1] = "Dirt Stain" // Defect Type Desc
            defectList[0][2] = "0" // Defect Type Value
            defectList[0][3] = "1" // Defect Category Id
            defectList[0][4] = "C01" // Defect Type Id*/

            /*for (i in 0..(timeline!!.length() - 1)) {
                for (j in 0..(JSONArray(JSONObject(timeline[i].toString())["defects"].toString()).length() - 1)) {
                    var array = arrayOf<String>()
                    for (k in 0..4) {
                        array += ""
                    }
                    defectList += array

                    jsonObject =
                        JSONObject(JSONArray(JSONObject(timeline[i].toString())["defects"].toString())[j].toString())
                    defectList[rowIndex][0] =
                        JSONObject(timeline[i].toString())["desc1"].toString() // Defect Category Desc
                    defectList[rowIndex][1] = jsonObject["desc1"].toString() // Defect Type Desc

                    if (rejectList.size > 0) {
                        for (arrayReject in rejectList) {
                            if (arrayReject[1] == jsonObject["defect"].toString()) {
                                defectList[rowIndex][2] = arrayReject[2] // Defect Type Value
                                break
                            } else {
                                defectList[rowIndex][2] = "0" // Defect Type Value
                            }
                        }
                    } else {
                        defectList[rowIndex][2] = "0" // Defect Type Value
                    }

                    defectList[rowIndex][3] =
                        JSONObject(timeline[i].toString())["defectCat"].toString() // Defect Category Id
                    defectList[rowIndex][4] = jsonObject["defect"].toString() // Defect Type Id

                    rowIndex++
                }
            }*/

            for (i in 0..(timeline!!.length() - 1)) {
                var array = arrayOf<String>()
                for (k in 0..4) {
                    array += ""
                }
                defectList += array

                jsonObject = JSONObject(timeline[i].toString())
                defectList[rowIndex][0] = jsonObject["defectCatDesc"].toString() // Defect Category Desc
                defectList[rowIndex][1] = jsonObject["defectDesc"].toString() // Defect Type Desc
                if (rejectList.size > 0) {
                    for (arrayReject in rejectList) {
                        if (arrayReject[1] == jsonObject["defect"].toString()) {
                            defectList[rowIndex][2] = arrayReject[2] // Defect Type Value
                            break
                        } else {
                            defectList[rowIndex][2] = "0" // Defect Type Value
                        }
                    }
                } else {
                    defectList[rowIndex][2] = "0" // Defect Type Value
                }
                defectList[rowIndex][3] = jsonObject["defectCat"].toString() // Defect Category Id
                defectList[rowIndex][4] = jsonObject["defect"].toString() // Defect Type Id

                rowIndex++
            }
        }
    }

    private fun populateCategory() {

        gridLayout_Category.removeAllViewsInLayout()

        var defectCategory: String = ""
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(5, 5, 5, 0)
        layoutParams.height = convertToDP(55)
        layoutParams.width = convertToDP(165)

        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.color = getColorStateList(buttonBackgroundColor)
        /*gradientDrawable.setStroke(2, Color.RED)*/
        gradientDrawable.cornerRadius = 7.0f


        for (array in defectList) {
            if (array[0] == defectCategory) continue

            defectCategory = array[0]
            var button_DefectCategory = Button(ContextThemeWrapper(this, buttonStyle), null, buttonStyle)
            button_DefectCategory.text = array[0]
            button_DefectCategory.layoutParams = layoutParams
            button_DefectCategory.background = gradientDrawable

            button_DefectCategory.setOnClickListener {

                populateTypes(button_DefectCategory.text.toString())
            }

            var linearLayout_DefectCategory = LinearLayout(this)
            linearLayout_DefectCategory.addView(button_DefectCategory)
            gridLayout_Category.addView(linearLayout_DefectCategory)

            updateCategoryCount(array[0])
        }
    }

    private fun populateTypes(defectCategory: String) {

        gridLayout_Types.removeAllViewsInLayout()

        val gradientDrawable_DefectCategory = GradientDrawable()
        gradientDrawable_DefectCategory.shape = GradientDrawable.RECTANGLE
        gradientDrawable_DefectCategory.color = getColorStateList(buttonBackgroundColor)
        /*gradientDrawable_DefectCategory.setStroke(2, Color.RED)*/
        gradientDrawable_DefectCategory.cornerRadius = 7.0f

        val gradientDrawable_DefectCategoryClicked = GradientDrawable()
        gradientDrawable_DefectCategoryClicked.shape = GradientDrawable.RECTANGLE
        gradientDrawable_DefectCategoryClicked.color = getColorStateList(buttonBackgroundColorDefectCategoryClicked)
        /*gradientDrawable_DefectCategoryClicked.setStroke(2, Color.RED)*/
        gradientDrawable_DefectCategoryClicked.cornerRadius = 7.0f

        for (i in 0..(gridLayout_Category.childCount - 1)) {
            if (((gridLayout_Category.getChildAt(i) as LinearLayout).getChildAt(0) as Button).text.contains(defectCategory)) {
                ((gridLayout_Category.getChildAt(i) as LinearLayout).getChildAt(0) as Button).background = gradientDrawable_DefectCategoryClicked
            } else {
                ((gridLayout_Category.getChildAt(i) as LinearLayout).getChildAt(0) as Button).background = gradientDrawable_DefectCategory
            }
        }

        val layoutParamsIncrement = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParamsIncrement.setMargins(20, 5, 1, 5)
        layoutParamsIncrement.height = convertToDP(70)
        layoutParamsIncrement.width = convertToDP(140)

        val layoutParamsTextView = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParamsTextView.setMargins(1, 5, 1, 5)
        layoutParamsTextView.height = convertToDP(70)
        layoutParamsTextView.width = convertToDP(60)

        val layoutParamsDecrement = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParamsDecrement.setMargins(1, 5, 0, 5)
        layoutParamsDecrement.height = convertToDP(70)
        layoutParamsDecrement.width = convertToDP(60)

        val gradientDrawableButtonIncrement = GradientDrawable()
        gradientDrawableButtonIncrement.shape = GradientDrawable.RECTANGLE
        gradientDrawableButtonIncrement.color = getColorStateList(buttonBackgroundColor)
        /*gradientDrawableButton.setStroke(2, Color.RED)*/
        gradientDrawableButtonIncrement.cornerRadius = 7.0f

        val gradientDrawableButtonDecrement = GradientDrawable()
        gradientDrawableButtonDecrement.shape = GradientDrawable.RECTANGLE
        gradientDrawableButtonDecrement.color = getColorStateList(buttonBackgroundColor)
        /*gradientDrawableButton.setStroke(2, Color.RED)*/
        gradientDrawableButtonDecrement.cornerRadius = 7.0f

        val gradientDrawableButtonIncrementClicked = GradientDrawable()
        gradientDrawableButtonIncrementClicked.shape = GradientDrawable.RECTANGLE
        gradientDrawableButtonIncrementClicked.color = getColorStateList(buttonBackgroundColorDefectTypeIncrementClicked)
        /*gradientDrawableButton.setStroke(2, Color.RED)*/
        gradientDrawableButtonIncrementClicked.cornerRadius = 7.0f

        val gradientDrawableTextView = GradientDrawable()
        gradientDrawableTextView.shape = GradientDrawable.RECTANGLE
        gradientDrawableTextView.setStroke(2, getColorStateList(R.color.global_button_background_color))
        gradientDrawableTextView.cornerRadius = 7.0f

        for (array in defectList) {

            if (defectCategory.contains(array[0])) {
                val button_Increment = Button(ContextThemeWrapper(this, buttonStyle), null, buttonStyle)
                val textView_Count = TextView(this)
                val button_Decrement = Button(ContextThemeWrapper(this, buttonStyle), null, buttonStyle)

                button_Increment.text = array[1]
                button_Increment.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                button_Increment.layoutParams = layoutParamsIncrement
                button_Increment.gravity = Gravity.CENTER
                button_Increment.setSingleLine(false)
                if (array[2].toInt() == 0) {
                    button_Increment.background = gradientDrawableButtonIncrement
                } else {
                    button_Increment.background = gradientDrawableButtonIncrementClicked
                }
                button_Increment.setOnClickListener {
                    array[2] = (array[2].toInt() + 1).toString()
                    textView_Count.text = array[2]
                    updateCategoryCount(array[0])

                    button_Increment.background = gradientDrawableButtonIncrementClicked
                }
                var linearLayout_Increment = LinearLayout(this)
                linearLayout_Increment.addView(button_Increment)
                gridLayout_Types.addView(linearLayout_Increment)

                textView_Count.text = array[2]
                textView_Count.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
                textView_Count.setTextColor(getColorStateList(R.color.global_text_color))
                textView_Count.layoutParams = layoutParamsTextView
                textView_Count.gravity = Gravity.CENTER
                textView_Count.background = gradientDrawableTextView
                var linearLayout_Count = LinearLayout(this)
                linearLayout_Count.addView(textView_Count)
                gridLayout_Types.addView(linearLayout_Count)

                button_Decrement.text = "-"
                button_Decrement.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
                button_Decrement.layoutParams = layoutParamsDecrement
                button_Decrement.gravity = Gravity.CENTER
                button_Decrement.background = gradientDrawableButtonDecrement
                button_Decrement.setOnClickListener {
                    if (array[2].toInt() > 0) {
                        array[2] = (array[2].toInt() - 1).toString()
                        textView_Count.text = array[2]
                        updateCategoryCount(array[0])

                        if (array[2].toInt() == 0) {
                            button_Increment.background = gradientDrawableButtonDecrement
                        }
                    }
                }
                var linearLayout_Decrement = LinearLayout(this)
                linearLayout_Decrement.addView(button_Decrement)
                gridLayout_Types.addView(linearLayout_Decrement)

            } else continue
        }

    }

    private fun updateCategoryCount(category: String) {

        var count: Int = 0

        for (array in defectList) {
            if (array[0] == category && array[2].toInt() > 0) {
                count += array[2].toInt()
            }

            if (array[2].toInt() > 0) {
                countAllReject += array[2].toInt()
            }
        }

        for (i in 0..(gridLayout_Category.childCount - 1)) {
            if (((gridLayout_Category.getChildAt(i) as LinearLayout).getChildAt(0) as Button).text.contains(category)) {
                if (count > 0) {
                    ((gridLayout_Category.getChildAt(i) as LinearLayout).getChildAt(0) as Button).text = category + " ($count)"
                } else {
                    ((gridLayout_Category.getChildAt(i) as LinearLayout).getChildAt(0) as Button).text = category
                }
            }
        }

        if (countAllReject > 0) {
            if (maxCardStatus.isNotEmpty()) {
                textView_CardDefectStatus!!.background.setColorFilter(
                    Color.parseColor(maxCardStatus),
                    PorterDuff.Mode.SRC_ATOP
                )
            }
        } else {
            if (currentCardStatus.isNotEmpty()) {
                textView_CardDefectStatus!!.background.setColorFilter(
                    Color.parseColor(currentCardStatus),
                    PorterDuff.Mode.SRC_ATOP
                )
            }
        }

    }

    private fun resetDefects() {

        getHeaderList()
        countAllReject = 0
        for (array in defectList) {
            array[2] = "0"
        }

        for (i in 0..(gridLayout_Category.childCount - 1)) {
            for (array in defectList) {
                if (((gridLayout_Category.getChildAt(i) as LinearLayout).getChildAt(0) as Button).text.contains(array[0])) {
                    ((gridLayout_Category.getChildAt(i) as LinearLayout).getChildAt(0) as Button).text = array[0]
                }
            }
        }

        ((gridLayout_Category.getChildAt(0) as LinearLayout).getChildAt(0) as Button).performClick()
    }

    private fun postGood() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getCaption("Do you want to proceed?"))
        builder.setCancelable(true)

        builder.setNegativeButton(
            getCaption("No"),
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel()})

        builder.setPositiveButton(
            getCaption("Yes"),
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel()
                postInlineHdr(true)
            }
        )

        val alert = builder.create()
        alert.show()
    }

    private fun postInlineHdr(isGood: Boolean) {

        pMONo = textView_MONo.text.toString()
        pLayNo = textView_LayNo.text.toString()
        pBatchNo = textView_BatchNo.text.toString()
        pSeq = (textView_InspectionCount.text.toString().toInt() + 1).toString()
        pPTNo = textView_PTNo.text.toString()
        pQty = 1

        var jsonParams = JSONObject()
        jsonParams.put("factory", GlobalVariable.factory)
        jsonParams.put("moNo", pMONo)
        jsonParams.put("layNo", pLayNo)
        jsonParams.put("bdlNo", bdlNo)
        jsonParams.put("mfgLine", GlobalVariable.mfgLine)
        jsonParams.put("empId", workerId)
        jsonParams.put("operation", operationCode)
        jsonParams.put("machine", machineCode)
        jsonParams.put("batchNo", pBatchNo)
        jsonParams.put("seq", pSeq)
        jsonParams.put("ptNo", pPTNo)
        jsonParams.put("qty", pQty)
        jsonParams.put("good", isGood)

        var count: Int
        if (!isGood) {
            count = maxCardCount
        } else if (currentCardCount > 1) {
            count = currentCardCount - 1
        } else {
            count = 1
        }

        jsonParams.put("count", count)
        jsonParams.put("createdBy", GlobalVariable.userId)

        var entity: StringEntity = StringEntity(jsonParams.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        api.post(this, GlobalVariable.urlInternal + ":3001/api/v1/qualityconnect/postInlineQCHdrSP",
            entity, "application/json", object : JsonHttpResponseHandler() {

            override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray) {
                try {
                    if (statusCode == 200) {

                        postIlHdrToExternal(timeline.toString())

                        if (!isGood) {
                            postInlineDet()
                        } else {
                            var result = JSONObject(timeline[0].toString())["result"].toString()
                            if (result == "proceed")
                                getHeaderList()
                            else if (result == "exit") {
                                val intent = Intent(this@DefectActivity, InlineLoadOnActivity::class.java)
                                startActivity(intent)
                            }
                        }

                        /*if (GlobalVariable.postToExternal) {
                            postElHdrToExternal(timeline.toString(), true)
                        }*/

                        /*if (textView_BundleBal.text.toString().toInt() == 1) {
                            val intent = Intent(this@DefectActivity, EndlineLoadOnActivity::class.java)
                            startActivity(intent)
                        } else {
                            resetDefects()
                        }*/
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                Toast.makeText(this@DefectActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                Toast.makeText(this@DefectActivity, responseString, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun postInlineDet() {
        var defectQty: Int
        var defectCategory: String
        var defectType: String

        for (array in defectList) {
            if (array[2].toInt() > 0) {

                defectCategory = array[3]
                defectType = array[4]
                defectQty = array[2].toInt()

                var jsonParams = JSONObject()
                jsonParams.put("factory", GlobalVariable.factory)
                jsonParams.put("moNo", textView_MONo.text)
                jsonParams.put("layNo", textView_LayNo.text)
                jsonParams.put("bdlNo", bdlNo)
                jsonParams.put("mfgLine", GlobalVariable.mfgLine)
                jsonParams.put("empId", workerId)
                jsonParams.put("operation", operationCode)
                jsonParams.put("machine", machineCode)
                jsonParams.put("batchNo", textView_BatchNo.text)
                jsonParams.put("seq", (textView_InspectionCount.text.toString().toInt() + 1).toString())
                jsonParams.put("defect", defectType)
                jsonParams.put("defectCat", defectCategory)
                jsonParams.put("qty", defectQty)
                jsonParams.put("createdBy", GlobalVariable.userId)

                var entity: StringEntity = StringEntity(jsonParams.toString())
                entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

                api.post(this@DefectActivity, GlobalVariable.urlInternal + ":3001/api/v1/qualityconnect/postInlineQCDetSP",
                    entity, "application/json", object : JsonHttpResponseHandler() {
                    override fun onSuccess(statusCode: Int, headers: Array<Header>?, response: JSONObject) {}

                    override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray) {
                        try {
                            if (statusCode == 200) {

                                var result = JSONObject(timeline[0].toString())["result"].toString()
                                if (result == "proceed") {
                                    getHeaderList()
                                } else if (result == "exit" || result == "authorize") {
                                    val intent = Intent(this@DefectActivity, InlineLoadOnActivity::class.java)
                                    startActivity(intent)
                                } /*else if (result == "authorize") {
                                    val intent = Intent(this@DefectActivity, AuthorizationActivity::class.java)
                                    intent.putExtra("workerId", workerId)
                                    intent.putExtra("ptNo", ptNo)
                                    intent.putExtra("operationCode", operationCode)
                                    intent.putExtra("machineCode", machineCode)
                                    startActivity(intent)
                                }*/

                                if (GlobalVariable.postToExternal) {
                                    postIlDtlToExternal(timeline.toString())
                                }

                                resetDefects()
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                        Toast.makeText(this@DefectActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
                    }

                    override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                        Toast.makeText(this@DefectActivity, responseString, Toast.LENGTH_LONG).show()
                    }
                })

                /*if (GlobalVariable.postToExternal) {
                    postIlDtlToExternal(timeline.toString())
                }*/
            }
        }
    }

    private fun postIlHdrToExternal(timeline: String) {
        var jsonObject = JSONObject(JSONArray(timeline)[0].toString())

        var jsonParams = JSONObject()
        jsonParams.put("pSewMacId", jsonObject["pMachine"].toString())
        jsonParams.put("pOperation", jsonObject["pOperation"].toString())
        jsonParams.put("pWorkerId", jsonObject["pEmpId"].toString())
        jsonParams.put("pPTNo", jsonObject["pPTNo"].toString())
        jsonParams.put("pProdLine", jsonObject["pMfgLine"].toString())
        jsonParams.put("pBatch", jsonObject["pBatchNo"].toString())
        jsonParams.put("pSeq", jsonObject["pSeq"].toString())
        jsonParams.put("pCreatedBy", jsonObject["pCreatedBy"].toString())
        jsonParams.put("pCreatedDt", jsonObject["pCreatedDt"].toString())


        var entity: StringEntity = StringEntity(jsonParams.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        api.post(this, GlobalVariable.urlExternal + "/DBWebAPI/postIlQCGarmSP/jsonp",
                 entity, "application/json", object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, response: JSONObject) {}

            override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray) {
                try {
                    if (statusCode == 200) {

                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                Toast.makeText(this@DefectActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                Toast.makeText(this@DefectActivity, responseString, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun postReject() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getCaption("Do you want to proceed?"))
        builder.setCancelable(true)

        builder.setNegativeButton(
            getCaption("No"),
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel()})

        builder.setPositiveButton(
            getCaption("Yes"),
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel()
                postInlineHdr(false)
            }
        )

        val alert = builder.create()
        alert.show()

    }

    private fun postIlDtlToExternal(timeline: String) {
        var jsonObject = JSONObject(JSONArray(timeline)[0].toString())

        var jsonParams = JSONObject()
        jsonParams.put("pSewMacId", jsonObject["pMachine"].toString())
        jsonParams.put("pOperation", jsonObject["pOperation"].toString())
        jsonParams.put("pWorkerId", jsonObject["pEmpId"].toString())
        jsonParams.put("pPTNo", jsonObject["pPTNo"].toString())
        jsonParams.put("pProdLine", jsonObject["pMfgLine"].toString())
        jsonParams.put("pBatch", jsonObject["pBatchNo"].toString())
        jsonParams.put("pSeq", jsonObject["pSeq"].toString())
        jsonParams.put("pCreatedBy", jsonObject["pCreatedBy"].toString())
        jsonParams.put("pCreatedDt", jsonObject["pCreatedDt"].toString())
        jsonParams.put("pDefectType", jsonObject["pDefectType"].toString())
        jsonParams.put("pDefectCat", jsonObject["pDefectCat"].toString())
        jsonParams.put("pDefectQty", jsonObject["pQty"].toString())

        var entity: StringEntity = StringEntity(jsonParams.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        api.post(this, GlobalVariable.urlExternal + "/DBWebAPI/postIlQCRejectGarmSP/jsonp",
                 entity, "application/json", object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, response: JSONObject) {}

            override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray) {
                try {
                    if (statusCode == 200) {

                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                Toast.makeText(this@DefectActivity, errorResponse.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                Toast.makeText(this@DefectActivity, responseString, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun convertToDP(value: Int): Int {
        var result = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), getResources().getDisplayMetrics())
        return result.toInt()
    }

    override fun onResume() {
        registerNetworkBroadcastReceiver(this@DefectActivity)
        super.onResume()
        isActivityVisible = true
    }

    override fun onPause() {
        unregisterNetworkBroadcastReceiver(this@DefectActivity)
        super.onPause()
        finish()
        isActivityVisible = false
    }

    override fun onBackPressed() {
        //super.onBackPressed()
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
