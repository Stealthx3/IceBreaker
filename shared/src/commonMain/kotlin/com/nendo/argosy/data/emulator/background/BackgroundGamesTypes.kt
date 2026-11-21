package com.nendo.argosy.data.emulator.background

/**
 * An app with a task on screen or in the background. [game] is set when the app is an emulator
 * still holding the game a session of ours ended in.
 */
data class RunningApp(
    val taskId: Int,
    val packageName: String,
    val displayId: Int,
    val rssKb: Long?,
    val game: BackgroundGame?
)
