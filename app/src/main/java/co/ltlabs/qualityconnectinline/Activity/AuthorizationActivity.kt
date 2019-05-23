package co.ltlabs.qualityconnectinline.Activity

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import co.ltlabs.qualityconnectinline.Utility.API
import co.ltlabs.qualityconnectinline.Utility.GlobalVariable
import co.ltlabs.qualityconnectinline.R
import com.loopj.android.http.JsonHttpResponseHandler
import kotlinx.android.synthetic.main.activity_authorization.*
import kotlinx.android.synthetic.main.activity_authorization.button_Submit
import org.apache.http.Header
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicHeader
import org.apache.http.protocol.HTTP
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class AuthorizationActivity : AppCompatActivity() {

    var api = API()
    var machineCode: String = ""
    var operationCode: String = ""
    var ptNo: String = ""
    var workerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authorization)

        var intent: Intent = intent
        workerId = intent.getStringExtra("workerId")
        ptNo = intent.getStringExtra("ptNo")
        operationCode = intent.getStringExtra("operationCode")
        machineCode = intent.getStringExtra("machineCode")

        initializeComponent()
    }

    private fun initializeComponent() {

        button_Back.setOnClickListener {
            exitPage()
        }

        button_Submit.setOnClickListener {
            if (
                textInput_Username.editText?.text.toString().isNotEmpty() &&
                textInput_Password.editText?.text.toString().isNotEmpty() &&
                editText_Remarks.text.toString().isNotEmpty()
            ) {
                var jsonParams = JSONObject()
                jsonParams.put("factory", GlobalVariable.factory)
                jsonParams.put("mfgLine", GlobalVariable.mfgLine)
                jsonParams.put("authUser", textInput_Username.editText?.text.toString())
                jsonParams.put("authPassword", textInput_Password.editText?.text.toString())
                jsonParams.put("authRemarks",editText_Remarks.text.toString())
                jsonParams.put("empId", workerId)
                jsonParams.put("ptNo", ptNo)
                jsonParams.put("operation", operationCode)
                jsonParams.put("machine", machineCode)

                var entity: StringEntity = StringEntity(jsonParams.toString())
                entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

                api.post(this@AuthorizationActivity, GlobalVariable.urlInternal + ":3001/api/v1/qualityconnect/postInlineQCAuthSP",
                    entity, "application/json", object : JsonHttpResponseHandler() {
                    override fun onSuccess(statusCode: Int, headers: Array<Header>?, timeline: JSONArray) {
                        try {
                            if (statusCode == 200) {

                                var result = JSONObject(timeline[0].toString())["result"].toString()
                                if (result == "success") {
                                    var intent = Intent(this@AuthorizationActivity, InlineLoadOnActivity::class.java)
                                    startActivity(intent)
                                } else if (result == "invalid") {
                                    Toast.makeText(this@AuthorizationActivity, "Invalid username/ password.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, errorResponse: JSONObject) {
                        println("$statusCode - $errorResponse")
                        var errorMessage = JSONObject(errorResponse.getString("error")).getString("message")
                        Toast.makeText(this@AuthorizationActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }

                    override fun onFailure(statusCode: Int, headers: Array<Header>, responseString: String, throwable: Throwable) {
                        println("$statusCode - $responseString")
                    }
                })
            }  else {
                Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
            }
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
        builder.setMessage(getCaption("Do you want to proceed?"))
        builder.setCancelable(true)

        builder.setNegativeButton(
            getCaption("No"),
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel()})

        builder.setPositiveButton(
            getCaption("Yes"),
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel()
                val intent = Intent(this@AuthorizationActivity, InlineLoadOnActivity::class.java)
                startActivity(intent)
            }
        )

        val alert = builder.create()
        alert.show()
    }
}
