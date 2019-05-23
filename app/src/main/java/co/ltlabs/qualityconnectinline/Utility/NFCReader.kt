package co.ltlabs.qualityconnectinline.Utility

import android.content.Context
import android.media.RingtoneManager
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager

import android.serialport.Application
import android.serialport.SerialPort

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import kotlin.experimental.and

class NFCReader {
    var uid: String = ""
    var context: Context? = null
    var mHandler1: Handler? = null

    var mApplication: Application? = null
    var mSerialPort: SerialPort? = null
    private var mInputStream: InputStream? = null
    private var mOutputStream: OutputStream? = null
    @Volatile
    var threadFlagLogin = true

    private var display = ""
    private val data = ByteArray(64)
    private val count = 0
    private var size = 0


    var _allowTapLogin = true
    private var serialPort: String = ""

    var rfidThread: Thread? = null

    fun run(_context: Context, _application: Application, _mHandler1: Handler) {

        if (GlobalVariable.hasBuiltInReader) {
            context = _context
            mApplication = _application
            mHandler1 = _mHandler1

            _allowTapLogin = true
            threadFlagLogin = true

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            serialPort = prefs.getString("pref_serial_conn", "/dev/ttyS2")

            try {
                mSerialPort = mApplication!!.getSerialPort(serialPort)
                mOutputStream = mSerialPort!!.outputStream
                StartAutoReadCommandNFC()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        if (GlobalVariable.hasBuiltInReader) {
            _allowTapLogin = false
            threadFlagLogin = false
            mSerialPort = null
            mApplication = null
            mHandler1 = null
            rfidThread!!.interrupt()
        }
    }

    private fun StartAutoReadCommandNFC() {
        val outData = toArray("80 04 07 03 01 00")
        val stx = toArray("20 00")
        val bcc = toArray("7E 03")
        val outBuffer = ByteArray(4 + outData.size)

        if (stx.size == 2 && bcc.size == 2) {

            System.arraycopy(stx, 0, outBuffer, 0, stx.size)
            System.arraycopy(outData, 0, outBuffer, 2, outData.size)
            System.arraycopy(bcc, 0, outBuffer, outBuffer.size - 2, bcc.size)

            try {
                mOutputStream!!.write(outBuffer, 0, outBuffer.size)
                for (i in outBuffer.indices) {
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            //println("sum error")
        }
    }

    fun toArray(cmd: String): ByteArray {
        var cmd = cmd
        cmd = cmd.trim { it <= ' ' }.replace(" ", "")
        val cmd_length = cmd.length / 2
        val outBuffer = ByteArray(cmd_length)

        if (cmd_length > 0) {
            for (i in 0 until cmd_length) {
                val k = cmd.substring(i * 2, (i + 1) * 2)
                try {
                    outBuffer[i] = Integer.parseInt(k, 16).toByte()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return outBuffer
    }

    /*@SuppressLint("HandlerLeak")
    private val mHandler1 = object : Handler() {
        override fun handleMessage(msg: Message) {
            //println("--->handleMessage Login = " + msg.obj)
            //super.handleMessage(msg);


            if (BigInteger.valueOf(java.lang.Long.parseLong(msg.obj.toString())).toString() == "0") return

            when (msg.what) {
                4 ->
                    // play notifcation sound
                    if (_allowTapLogin) {
                        if (threadFlagLogin) {
                            try {

                                r.play()


                                uid = msg.obj.toString()

                                *//*var test: Activity = context as Activity
                                test.onRes*//*
                                val test = context as AppCompatActivity




                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }
                    } else {
                        Toast.makeText(context, "Tap id to login feature disabled.", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }*/

    fun handleMessage(msg: Message) {
        if (GlobalVariable.hasBuiltInReader) {
            if (BigInteger.valueOf(java.lang.Long.parseLong(msg.obj.toString())).toString() == "0") return

            when (msg.what) {
                4 ->
                    // play notifcation sound
                    if (_allowTapLogin) {
                        if (threadFlagLogin) {
                            try {
                                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                val r = RingtoneManager.getRingtone(context, notification)
                                r.play()

                                if (msg.obj.toString().length > 10) {
                                    uid = msg.obj.toString().takeLast(10)
                                } else {
                                    uid = msg.obj.toString()
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }
                    } else {
                        //Toast.makeText(context, "Tap id to login feature disabled.", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    fun onResume() {
        if (GlobalVariable.hasBuiltInReader) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            serialPort = prefs.getString("pref_serial_conn", "/dev/ttyS2")

            try {
                mSerialPort = mApplication!!.getSerialPort(serialPort)
                mOutputStream = mSerialPort!!.outputStream
                threadFlagLogin = _allowTapLogin//true;
                ReadThread()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun ReadThread() {
        mInputStream = mSerialPort!!.inputStream
        rfidThread = Thread(Runnable {
            while (threadFlagLogin) {
                if (mInputStream != null) {
                    try {
                        val receiveBuffer = ByteArray(64)
                        val msg = Message()

                        size = mInputStream!!.read(receiveBuffer)

                        display = bytes2HexString(
                            receiveBuffer,
                            size
                        )
                        //println("size=$size")
                        System.arraycopy(receiveBuffer, 0, data, count, size)

                        val id = byteArrayOf(data[11], data[10], data[9], data[8])
                        display = bytes2HexString(id, 4)

                        display = java.lang.Long.parseLong(display.replace(" ", ""), 16).toString()
                        if (display.length < 10) {
                            // pad 0 if less than 10
                            for (i in 0..10 - display.length) {
                                display = "0$display"
                            }
                        }

                        msg.what = 4
                        msg.obj = display
                        mHandler1?.sendMessage(msg) // to display data
                        display = ""

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })

        rfidThread!!.start()
    }

    companion object {

        fun bytes2HexString(b: ByteArray, size: Int): String {
            var hex: String? = ""
            var out = ""
            for (i in 0 until size) {
                //hex = Integer.toHexString(b[i] and 0xFF)
                hex = String.format("%02X", (b[i] and 0xFF.toByte()))


                if (hex != null) {
                    if (hex.length == 1) {
                        hex = '0' + hex
                    }
                    out += "$hex "
                }

            }

            return out.toUpperCase()
        }
    }

    fun onBackPressed() {
        if (GlobalVariable.hasBuiltInReader) {
            stop()
        }
    }
}