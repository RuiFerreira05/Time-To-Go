package com.timetogo.app.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.timetogo.app.R

class PlacePredictionAdapter(
    private val onPredictionClicked: (AutocompletePrediction) -> Unit
) : RecyclerView.Adapter<PlacePredictionAdapter.PredictionViewHolder>() {

    private var predictions: List<AutocompletePrediction> = emptyList()

    fun submitList(newPredictions: List<AutocompletePrediction>) {
        predictions = newPredictions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place_prediction, parent, false)
        return PredictionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PredictionViewHolder, position: Int) {
        val prediction = predictions[position]
        holder.bind(prediction)
    }

    override fun getItemCount(): Int = predictions.size

    inner class PredictionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textPrimary: TextView = itemView.findViewById(R.id.text_primary)
        private val textSecondary: TextView = itemView.findViewById(R.id.text_secondary)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPredictionClicked(predictions[position])
                }
            }
        }

        fun bind(prediction: AutocompletePrediction) {
            textPrimary.text = prediction.getPrimaryText(null)
            textSecondary.text = prediction.getSecondaryText(null)
        }
    }
}
