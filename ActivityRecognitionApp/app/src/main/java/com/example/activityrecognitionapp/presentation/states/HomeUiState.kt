package com.example.activityrecognitionapp.presentation.states

data class HomeUiState(
    val stand: Int = 0,
    val walk: Int = 0,
    val run: Int = 0,
    val total: Int = 0,
//    val standPercentage : Float =  (stand/total).toFloat(),
//    val walkPercentage : Float =  (stand/total).toFloat(),
//    val runPercentage : Float =  (stand/total).toFloat()

) {
    val standPercentage: Float
        get() = if (total > 0) stand / total.toFloat() else 0f

    val walkPercentage: Float
        get() = if (total > 0) walk / total.toFloat() else 0f

    val runPercentage: Float
        get() = if (total > 0) run / total.toFloat() else 0f
}
