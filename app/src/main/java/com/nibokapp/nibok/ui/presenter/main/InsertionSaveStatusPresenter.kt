package com.nibokapp.nibok.ui.presenter.main

import android.util.Log
import com.nibokapp.nibok.domain.command.bookinsertion.savestatus.CheckBookInsertionSaveStatusCommand
import com.nibokapp.nibok.domain.command.bookinsertion.savestatus.ToggleBookInsertionSaveStatusCommand

/**
 * Interface for presenters that can check or change the save status of an insertion.
 */
interface InsertionSaveStatusPresenter {

    /**
     * Check if the insertion with the given id is saved or not.
     *
     * @param insertionId the id of the insertion
     *
     * @return true if the insertion is saved, false otherwise
     */
    fun isInsertionSaved(insertionId: String): Boolean {
        val saved = CheckBookInsertionSaveStatusCommand(insertionId).execute()
        Log.d("InsertionSavePresenter", "Insertion $insertionId is saved: $saved")
        return saved
    }

    /**
     * Toggle the save status of the insertion with the given id.
     * If the insertion was saved it is unsaved and vice versa.
     *
     * @param insertionId the id of the insertion
     *
     * @return true if the insertion was saved, false if it was unsaved
     */
    fun toggleInsertionSave(insertionId: String): Boolean {
        val saved = ToggleBookInsertionSaveStatusCommand(insertionId).execute()
        Log.d("InsertionSavePresenter", "After toggle insertion $insertionId saved: $saved")
        return saved
    }

}
