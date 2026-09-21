package com.jericx.trainr.presentation.unstuck.feedback

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jericx.trainr.R
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.unstuck.AdjustmentFeedback
import com.jericx.trainr.domain.unstuck.AdjustmentProposal
import com.jericx.trainr.domain.unstuck.ChangeKind
import com.jericx.trainr.domain.unstuck.FeedbackAnswer
import com.jericx.trainr.presentation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class AdjustmentFeedbackUiState(
    val isLoaded: Boolean = false,
    val isReplacement: Boolean = false,
    val substituteName: String = "",
    val originalName: String = "",
    @StringRes val trendLabelRes: Int = R.string.trend_training_performance,
    val answer: FeedbackAnswer? = null,
    val guidanceKey: String? = null
) {
    val offersGuidance: Boolean
        get() = answer == FeedbackAnswer.EXERCISE_CONFUSING && guidanceKey != null
}

@HiltViewModel
class AdjustmentFeedbackViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val adjustmentRepository: AdjustmentRepository,
    private val userRepository: UserRepository,
    private val catalog: ExerciseCatalog
) : ViewModel() {

    private val adjustmentId: Long = savedStateHandle[Screen.Feedback.ARG_ADJUSTMENT_ID] ?: 0L

    private val _uiState = MutableStateFlow(AdjustmentFeedbackUiState())
    val uiState: StateFlow<AdjustmentFeedbackUiState> = _uiState.asStateFlow()

    private val _savedEvents = Channel<FeedbackAnswer?>(Channel.BUFFERED)
    val savedEvents: Flow<FeedbackAnswer?> = _savedEvents.receiveAsFlow()

    private var feedbackId = 0L
    private var isSaved = false

    init {
        viewModelScope.launch { load() }
    }

    fun answer(answer: FeedbackAnswer) = save(answer)

    fun dismiss() = save(null)

    // One row per adjustment, written once: a second tap must not record a
    // second answer or send the flow onward twice.
    private fun save(answer: FeedbackAnswer?) {
        if (isSaved) return
        isSaved = true

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            feedbackId = adjustmentRepository.saveFeedback(
                AdjustmentFeedback(
                    id = feedbackId,
                    adjustmentId = adjustmentId,
                    answer = answer,
                    answeredAt = now.takeIf { answer != null },
                    dismissedAt = now.takeIf { answer == null }
                )
            )
            _uiState.value = _uiState.value.copy(answer = answer)
            _savedEvents.send(answer)
        }
    }

    private suspend fun load() {
        val adjustment = adjustmentRepository.getAdjustmentById(adjustmentId)
        val stored = adjustmentRepository.getFeedback(adjustmentId)
        val goal = userRepository.getCurrentUser()?.fitnessGoal
        val replaced = adjustment?.proposal?.replacement()
        feedbackId = stored?.id ?: 0L

        _uiState.value = AdjustmentFeedbackUiState(
            isLoaded = true,
            isReplacement = replaced != null,
            substituteName = replaced?.after?.catalogKey?.let { catalog[it]?.name }.orEmpty(),
            originalName = replaced?.before?.catalogKey?.let { catalog[it]?.name }.orEmpty(),
            trendLabelRes = if (goal == FitnessGoal.STRENGTH) {
                R.string.trend_strength
            } else {
                R.string.trend_training_performance
            },
            answer = stored?.answer,
            guidanceKey = adjustment?.proposal?.guidanceKey()
        )
    }
}

private fun AdjustmentProposal.replacement() =
    changes.firstOrNull { it.kind == ChangeKind.REPLACE_UNPERFORMED }

// An omitted exercise is no longer part of the day, so its how-to cannot be
// opened: only a change that leaves something on screen can offer guidance.
private fun AdjustmentProposal.guidanceKey(): String? =
    replacement()?.after?.catalogKey
        ?: changes.firstOrNull { it.kind != ChangeKind.OMIT_UNPERFORMED }?.before?.catalogKey
