package co.ltlabs.qualityconnectinline.Adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import java.util.ArrayList
import android.graphics.PorterDuff
import android.widget.LinearLayout
import co.ltlabs.qualityconnectinline.Data.CardStatusList
import co.ltlabs.qualityconnectinline.R


class CardStatusAdapter(context: Context, textViewResourceId: Int) : ArrayAdapter<CardStatusList>(context, textViewResourceId) {
    private val cardStatusList = ArrayList<CardStatusList>()

    internal class RejectViewHolder {
        var textView_CardStatusOperator: TextView? = null
        var textView_CardStatusOperation: TextView? = null
        var textView_CardStatusMachine: TextView? = null
        var textView_CardStatusRejectCount: TextView? = null
        var linearLayout_CardStatusListItem: LinearLayout? = null
    }

    override fun add(obj: CardStatusList) {
        cardStatusList.add(obj)
        super.add(obj)
    }

    override fun getCount(): Int {
        return this.cardStatusList.size
    }

    override fun getItem(index: Int): CardStatusList? {
        return this.cardStatusList[index]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var row = convertView
        val viewHolder: RejectViewHolder
        if (row == null) {
            val inflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            row = inflater.inflate(R.layout.list_item_card_status, parent, false)
            viewHolder = RejectViewHolder()
            viewHolder.textView_CardStatusOperator = row!!.findViewById(R.id.textView_CardStatusOperator) as TextView
            viewHolder.textView_CardStatusOperation = row.findViewById(R.id.textView_CardStatusOperation) as TextView
            viewHolder.textView_CardStatusMachine = row.findViewById(R.id.textView_CardStatusMachine) as TextView
            viewHolder.textView_CardStatusRejectCount = row.findViewById(R.id.textView_CardStatusRejectCount) as TextView
            viewHolder.linearLayout_CardStatusListItem = row.findViewById(R.id.linearLayout_CardStatusListItem) as LinearLayout

            row.tag = viewHolder
        } else {
            viewHolder = row.tag as RejectViewHolder
        }

        val rejectList = getItem(position)
        viewHolder.textView_CardStatusOperator!!.setText(rejectList!!.textView_CardStatusOperator)
        viewHolder.textView_CardStatusOperation!!.setText(rejectList!!.textView_CardStatusOperation)
        viewHolder.textView_CardStatusMachine!!.setText(rejectList!!.textView_CardStatusMachine)
        viewHolder.textView_CardStatusRejectCount!!.setText(rejectList!!.textView_CardStatusRejectCount)

        viewHolder.linearLayout_CardStatusListItem!!.background.setColorFilter(
            Color.parseColor(rejectList!!.linearLayout_CardStatusListItem),
            PorterDuff.Mode.SRC_ATOP
        )

        return row
    }

    fun decodeToBitmap(decodedByte: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
    }

    companion object {
        private val TAG = "CardStatusArrayAdapter"
    }
}